package pl.databucket.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import pl.databucket.server.configuration.AppProperties;
import pl.databucket.server.dto.PublicProjectDto;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.User;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.repository.ProjectRepository;
import pl.databucket.server.repository.UserRepository;
import pl.databucket.server.service.mail.MailSenderHandler;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * New accounts (sign-up with a password, or through single sign-on) start without projects. While registering,
 * the person can pick projects marked as public (Management - Projects - Public) and write a message; the SUPER
 * admins get both by email and assign the projects in Management - Users. Nothing is stored.
 */
@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    public static final int MESSAGE_MAX = 500;

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final String mailHost;

    public RegistrationService(ProjectRepository projectRepository, UserRepository userRepository,
                               JavaMailSender mailSender, AppProperties appProperties,
                               @Value("${spring.mail.host:}") String mailHost) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.appProperties = appProperties;
        this.mailHost = mailHost;
    }

    /** Whether the SUPER admins can be told about a new account at all. */
    public boolean isMailConfigured() {
        return mailHost != null && !mailHost.isBlank();
    }

    /**
     * Projects a new user can ask for: public, enabled and not expired, sorted by name. Empty without mail,
     * as the request could not reach anyone.
     */
    public List<PublicProjectDto> getPublicProjects() {
        if (!isMailConfigured())
            return List.of();
        return publicProjects().stream()
                .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                .map(project -> new PublicProjectDto(project.getId(), project.getName(), project.getDescription()))
                .toList();
    }

    private List<Project> publicProjects() {
        return projectRepository.findAllByDeletedOrderById(false).stream()
                .filter(project -> Boolean.TRUE.equals(project.getPublicVisible()))
                .filter(project -> !Boolean.FALSE.equals(project.getEnabled()) && !project.isExpired())
                .toList();
    }

    /** The message as it is emailed: trimmed, at most MESSAGE_MAX characters, null when empty. */
    public static String cleanMessage(String message) {
        if (message == null || message.isBlank())
            return null;
        String trimmed = message.trim();
        return trimmed.length() > MESSAGE_MAX ? trimmed.substring(0, MESSAGE_MAX) : trimmed;
    }

    /**
     * Emails the enabled SUPER admins (with an email address) about a new account, with the public projects the
     * user picked (other ids are ignored) and the message. A failed email is only logged: the account exists anyway.
     *
     * @param how how the account was created, for the email and the audit log, e.g. "single sign-on"
     */
    public void notifySuperAdmins(User newUser, Collection<Integer> projectIds, String message, String how) {
        Set<Integer> wanted = projectIds != null ? Set.copyOf(projectIds) : Set.of();
        List<Project> projects = publicProjects().stream()
                .filter(project -> wanted.contains(project.getId()))
                .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        String cleanMessage = cleanMessage(message);

        List<User> superAdmins = isMailConfigured()
                ? userRepository.findAllByOrderById().stream()
                .filter(User::isSuperUser)
                .filter(user -> !Boolean.FALSE.equals(user.getEnabled()))
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .filter(user -> user.getId() != newUser.getId())
                .toList()
                : List.of();

        int sent = 0;
        if (!superAdmins.isEmpty()) {
            String text = emailText(newUser, projects, cleanMessage, how);
            for (User admin : superAdmins) {
                try {
                    MailSenderHandler mail = new MailSenderHandler(mailSender);
                    if (appProperties.getMailFrom() != null)
                        mail.setFrom(appProperties.getMailFrom());
                    mail.setTo(admin.getEmail());
                    mail.setSubject("Databucket - new user " + newUser.getUsername());
                    mail.setText(text, true);
                    mail.send();
                    sent++;
                } catch (Exception e) {
                    logger.warn("Failed to email the new user {} to {}: {}", newUser.getUsername(), admin.getUsername(), e.getMessage());
                }
            }
        }
        AuditLog.log("user.registered", "user={} via={} requested={} emailed={}/{}", newUser.getUsername(), how,
                projects.stream().map(Project::getName).collect(Collectors.joining(",")), sent, superAdmins.size());
    }

    static String emailText(User newUser, List<Project> projects, String message, String how) {
        StringBuilder text = new StringBuilder("<h2>Databucket - new user</h2>")
                .append("<p><b>").append(HtmlUtils.htmlEscape(newUser.getUsername())).append("</b>");
        if (newUser.getEmail() != null)
            text.append(" (").append(HtmlUtils.htmlEscape(newUser.getEmail())).append(")");
        text.append(" has created an account through ").append(HtmlUtils.htmlEscape(how)).append(".</p>");

        if (projects.isEmpty()) {
            text.append("<p>No project was chosen.</p>");
        } else {
            text.append("<p>Asks for access to:</p><ul>");
            projects.forEach(project -> text.append("<li>").append(HtmlUtils.htmlEscape(project.getName())).append("</li>"));
            text.append("</ul>");
        }
        if (message != null)
            text.append("<p>Message:</p><blockquote>").append(HtmlUtils.htmlEscape(message).replace("\n", "<br/>")).append("</blockquote>");

        text.append("<p>The user has no project yet. To give access, open <b>Management - Users</b> and assign projects; ")
                .append("then the admins of the project give access to buckets in the project's <b>Settings</b>.</p>");
        return text.toString();
    }
}
