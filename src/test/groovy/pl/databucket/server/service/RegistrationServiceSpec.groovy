package pl.databucket.server.service

import org.springframework.mail.javamail.JavaMailSender
import pl.databucket.server.configuration.AppProperties
import pl.databucket.server.entity.Project
import pl.databucket.server.entity.Role
import pl.databucket.server.entity.User
import pl.databucket.server.repository.ProjectRepository
import pl.databucket.server.repository.UserRepository
import spock.lang.Specification

class RegistrationServiceSpec extends Specification {

    ProjectRepository projectRepository = Mock()
    UserRepository userRepository = Mock()
    JavaMailSender mailSender = Mock()

    def "should list only public, enabled and current projects, sorted by name"() {
        given:
        projectRepository.findAllByDeletedOrderById(false) >> [
                project(1, "Zoo", true),
                project(2, "admin tools", true),
                project(3, "Private", false),
                project(4, "Disabled", true, false),
                project(5, "Expired", true, true, new Date(System.currentTimeMillis() - 86_400_000))]

        expect:
        service("smtp.example.com").getPublicProjects()*.name() == ["admin tools", "Zoo"]
    }

    def "should list no projects without mail, as nobody could be asked"() {
        given:
        projectRepository.findAllByDeletedOrderById(false) >> [project(1, "Shop", true)]

        expect:
        service("").getPublicProjects() == []
    }

    def "should email nobody without mail, but still log the new user"() {
        given:
        projectRepository.findAllByDeletedOrderById(false) >> []

        when:
        service("").notifySuperAdmins(new User(id: 9, username: "john"), [1], "hi", "sign-up")

        then:
        0 * userRepository._
        0 * mailSender._
    }

    def "should email only the enabled SUPER admins with an email"() {
        given:
        projectRepository.findAllByDeletedOrderById(false) >> []
        userRepository.findAllByOrderById() >> [
                user(1, "super", "SUPER", "super@example.com"),
                user(2, "noMail", "SUPER", null),
                user(3, "off", "SUPER", "off@example.com", false),
                user(4, "admin", "ADMIN", "admin@example.com")]

        when:
        service("smtp.example.com").notifySuperAdmins(new User(id: 9, username: "john"), [], null, "sign-up")

        then: "one email, to super"
        1 * mailSender.createMimeMessage() >> new jakarta.mail.internet.MimeMessage((jakarta.mail.Session) null)
        1 * mailSender.send(_)
    }

    def "should name the requested public projects in the email, escaped"() {
        given:
        def newUser = new User(username: "<john>", email: "john@example.com")

        when:
        def text = RegistrationService.emailText(newUser, [project(3, "Shop & Co", true)], "Hello <b>\nthere", "single sign-on")

        then:
        text.contains("<b>&lt;john&gt;</b> (john@example.com) has created an account through single sign-on.")
        text.contains("<li>Shop &amp; Co</li>")
        text.contains("Hello &lt;b&gt;<br/>there")
    }

    def "should shorten the message and drop an empty one"() {
        expect:
        RegistrationService.cleanMessage("  ") == null
        RegistrationService.cleanMessage(" hi ") == "hi"
        RegistrationService.cleanMessage("x" * 600).length() == RegistrationService.MESSAGE_MAX
    }

    private RegistrationService service(String mailHost) {
        new RegistrationService(projectRepository, userRepository, mailSender, new AppProperties(), mailHost)
    }

    private static Project project(int id, String name, boolean isPublic, boolean enabled = true, Date expiration = null) {
        def project = new Project()
        project.id = id
        project.name = name
        project.publicVisible = isPublic
        project.enabled = enabled
        project.expirationDate = expiration
        project
    }

    private static User user(long id, String name, String role, String email, boolean enabled = true) {
        new User(id: id, username: name, email: email, enabled: enabled, roles: [new Role(name: role)] as Set)
    }
}
