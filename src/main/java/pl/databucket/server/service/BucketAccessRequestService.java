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
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.User;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.repository.ProjectRepository;
import pl.databucket.server.repository.UserRepository;
import pl.databucket.server.security.CustomUserDetails;
import pl.databucket.server.service.mail.MailSenderHandler;

import java.util.List;

/**
 * Requests for access to the data of a bucket: a user without access asks on the Overview page (only for buckets
 * visible there) and the admins of the project get an email. Nothing is granted here and nothing is stored: an admin
 * gives the access in Settings (a team, a role or the users of the bucket, and the views), as for anyone else.
 * Without mail there is no way to ask, so the Overview page doesn't offer it.
 */
@Service
public class BucketAccessRequestService {

    private static final Logger logger = LoggerFactory.getLogger(BucketAccessRequestService.class);

    private final BucketService bucketService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final String mailHost;

    public BucketAccessRequestService(BucketService bucketService, UserService userService,
                                      UserRepository userRepository, ProjectRepository projectRepository,
                                      JavaMailSender mailSender, AppProperties appProperties,
                                      @Value("${spring.mail.host:}") String mailHost) {
        this.bucketService = bucketService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.mailSender = mailSender;
        this.appProperties = appProperties;
        this.mailHost = mailHost;
    }

    /**
     * The admins of the signed-in project who get the request of the user: enabled, with an email address, not the
     * user themselves. Empty when mail is not configured, so then the access can't be asked for.
     */
    public List<User> getAdminsToNotify(User requester) {
        if (mailHost == null || mailHost.isBlank())
            return List.of();
        Project project = projectRepository.findById(getProjectId()).orElse(null);
        if (project == null)
            return List.of();
        return userRepository.findUsersByProjectsContainsOrderById(project).stream()
                .filter(user -> user.isAdminUser() && !Boolean.FALSE.equals(user.getEnabled()))
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .filter(user -> user.getId() != requester.getId())
                .toList();
    }

    /**
     * Emails the admins of the project. Each click sends the emails again.
     *
     * @throws IllegalStateException with the reason the request can't be made
     */
    @Transactional(readOnly = true)
    public void request(String bucketName) {
        User user = userService.getCurrentUser();
        Bucket bucket = bucketService.getBucket(bucketName);
        if (bucket == null || !bucket.isOverviewVisible())
            throw new IllegalStateException("The bucket doesn't exist");
        if (bucketService.hasUserAccessToBucket(bucket, user))
            throw new IllegalStateException("You already have access to this bucket");
        List<User> admins = getAdminsToNotify(user);
        if (admins.isEmpty())
            throw new IllegalStateException("The admins of the project can't be asked by email. Contact them directly.");

        Project project = projectRepository.findById(getProjectId()).orElseThrow();
        String text = "<h2>Databucket - access request</h2>"
                + "<p><b>" + HtmlUtils.htmlEscape(user.getUsername()) + "</b>"
                + (user.getEmail() != null ? " (" + HtmlUtils.htmlEscape(user.getEmail()) + ")" : "")
                + " asks for access to the bucket <b>" + HtmlUtils.htmlEscape(bucket.getName())
                + "</b> in the project <b>" + HtmlUtils.htmlEscape(project.getName()) + "</b>.</p>"
                + "<p>To give the access, open <b>Settings</b> in the project and add the user to a team, "
                + "or to the users of the bucket and its views.</p>";

        int sent = 0;
        for (User admin : admins) {
            try {
                MailSenderHandler mail = new MailSenderHandler(mailSender);
                if (appProperties.getMailFrom() != null)
                    mail.setFrom(appProperties.getMailFrom());
                mail.setTo(admin.getEmail());
                mail.setSubject("Databucket - access to the bucket " + bucket.getName());
                mail.setText(text, true);
                mail.send();
                sent++;
            } catch (Exception e) {
                logger.warn("Failed to email the access request to {}: {}", admin.getUsername(), e.getMessage());
            }
        }
        AuditLog.log("access.request", "bucket={} user={} emailed={}/{}", bucket.getName(), user.getUsername(), sent, admins.size());
        if (sent == 0)
            throw new IllegalStateException("The email to the admins of the project could not be sent. Try again later or contact them directly.");
    }

    private static Integer getProjectId() {
        return ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getProjectId();
    }
}
