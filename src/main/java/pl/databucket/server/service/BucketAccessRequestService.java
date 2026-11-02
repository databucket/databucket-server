package pl.databucket.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import pl.databucket.server.configuration.AppProperties;
import pl.databucket.server.dto.BucketAccessRequestDto;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.BucketAccessRequest;
import pl.databucket.server.entity.BucketAccessRequest.Status;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.repository.BucketAccessRequestRepository;
import pl.databucket.server.repository.ProjectRepository;
import pl.databucket.server.repository.UserRepository;
import pl.databucket.server.security.CustomUserDetails;
import pl.databucket.server.service.mail.MailSenderHandler;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Requests for access to the data of a bucket: a user without access asks on the Overview page (only for buckets
 * visible there), an admin of the project approves (the user is added to the users of the bucket) or rejects.
 * Requests are project entities (Hibernate tenant).
 */
@Service
public class BucketAccessRequestService {

    private static final Logger logger = LoggerFactory.getLogger(BucketAccessRequestService.class);

    private final BucketAccessRequestRepository requestRepository;
    private final BucketService bucketService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    // the admins are emailed only when mail is configured; the requests are always in the application
    private final String mailHost;

    public BucketAccessRequestService(BucketAccessRequestRepository requestRepository, BucketService bucketService,
                                      UserService userService, UserRepository userRepository,
                                      ProjectRepository projectRepository, JavaMailSender mailSender,
                                      AppProperties appProperties, @Value("${spring.mail.host:}") String mailHost) {
        this.requestRepository = requestRepository;
        this.bucketService = bucketService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.mailSender = mailSender;
        this.appProperties = appProperties;
        this.mailHost = mailHost;
    }

    /**
     * @throws IllegalStateException with the reason the request can't be made
     */
    @Transactional
    public void request(String bucketName) {
        User user = userService.getCurrentUser();
        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null || !bucket.isOverviewVisible())
            throw new IllegalStateException("The bucket doesn't exist");
        if (bucketService.hasUserAccessToBucket(bucket, user))
            throw new IllegalStateException("You already have access to this bucket");
        if (requestRepository.existsByBucketAndUserAndStatus(bucket, user, Status.PENDING))
            throw new IllegalStateException("You have already asked for access to this bucket");

        BucketAccessRequest request = new BucketAccessRequest();
        request.setBucket(bucket);
        request.setUser(user);
        request.setRequestedAt(new Date());
        requestRepository.save(request);
        AuditLog.log("access.request", "bucket={} user={}", bucket.getName(), user.getUsername());

        notifyAdmins(bucket, user);
    }

    @Transactional(readOnly = true)
    public List<BucketAccessRequestDto> getPending() {
        return requestRepository.findAllByStatusOrderByRequestedAt(Status.PENDING).stream()
                .filter(request -> !request.getBucket().getDeleted())
                .map(BucketAccessRequestService::toDto)
                .toList();
    }

    public long countPending() {
        return requestRepository.countByStatusAndBucketDeleted(Status.PENDING, false);
    }

    @Transactional
    public void approve(long id) throws ItemNotFoundException {
        BucketAccessRequest request = pending(id);
        Bucket bucket = request.getBucket();
        if (bucket.getUsers() == null)
            bucket.setUsers(new HashSet<>());
        bucket.getUsers().add(request.getUser());
        decide(request, Status.APPROVED);
        AuditLog.log("access.approve", "bucket={} user={}", bucket.getName(), request.getUser().getUsername());
    }

    @Transactional
    public void reject(long id) throws ItemNotFoundException {
        BucketAccessRequest request = pending(id);
        decide(request, Status.REJECTED);
        AuditLog.log("access.reject", "bucket={} user={}", request.getBucket().getName(), request.getUser().getUsername());
    }

    private BucketAccessRequest pending(long id) throws ItemNotFoundException {
        return requestRepository.findById(id)
                .filter(request -> request.getStatus() == Status.PENDING)
                .orElseThrow(() -> new ItemNotFoundException(BucketAccessRequest.class, id));
    }

    private void decide(BucketAccessRequest request, Status status) {
        request.setStatus(status);
        request.setDecidedAt(new Date());
        request.setDecidedBy(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private static BucketAccessRequestDto toDto(BucketAccessRequest request) {
        BucketAccessRequestDto dto = new BucketAccessRequestDto();
        dto.setId(request.getId());
        dto.setBucketName(request.getBucket().getName());
        dto.setIconName(request.getBucket().getIconName());
        dto.setIconColor(request.getBucket().getIconColor());
        dto.setIconSvg(request.getBucket().getIconSvg());
        dto.setUsername(request.getUser().getUsername());
        dto.setEmail(request.getUser().getEmail());
        dto.setRequestedAt(request.getRequestedAt());
        return dto;
    }

    // an email to every admin of the project with an email address; a failure doesn't stop the request
    private void notifyAdmins(Bucket bucket, User requester) {
        if (mailHost == null || mailHost.isBlank())
            return;
        Integer projectId = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getProjectId();
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null)
            return;

        String text = "<h2>Databucket - access request</h2>"
                + "<p><b>" + HtmlUtils.htmlEscape(requester.getUsername()) + "</b> asks for access to the bucket <b>"
                + HtmlUtils.htmlEscape(bucket.getName()) + "</b> in the project <b>" + HtmlUtils.htmlEscape(project.getName()) + "</b>.</p>"
                + "<p>Open <b>Overview</b> in the project to approve or reject the request.</p>";

        userRepository.findUsersByProjectsContainsOrderById(project).stream()
                .filter(user -> user.isAdminUser() && !Boolean.FALSE.equals(user.getEnabled()))
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .filter(user -> user.getId() != requester.getId())
                .forEach(admin -> {
                    try {
                        MailSenderHandler mail = new MailSenderHandler(mailSender);
                        if (appProperties.getMailFrom() != null)
                            mail.setFrom(appProperties.getMailFrom());
                        mail.setTo(admin.getEmail());
                        mail.setSubject("Databucket - access to the bucket " + bucket.getName());
                        mail.setText(text, true);
                        mail.send();
                    } catch (Exception e) {
                        logger.warn("Failed to email the access request to {}: {}", admin.getUsername(), e.getMessage());
                    }
                });
    }
}
