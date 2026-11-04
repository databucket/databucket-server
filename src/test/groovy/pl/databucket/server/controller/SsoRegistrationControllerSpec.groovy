package pl.databucket.server.controller

import org.springframework.http.HttpStatus
import pl.databucket.server.configuration.SsoProperties
import pl.databucket.server.dto.PublicProjectDto
import pl.databucket.server.dto.SsoRegistrationReqDTO
import pl.databucket.server.entity.User
import pl.databucket.server.exception.ItemAlreadyExistsException
import pl.databucket.server.security.SsoCodeService
import pl.databucket.server.service.ManageUserService
import pl.databucket.server.service.RegistrationService
import spock.lang.Specification
import spock.lang.Subject

import java.lang.reflect.Field

class SsoRegistrationControllerSpec extends Specification {

    SsoProperties ssoProperties = new SsoProperties(enabled: true, selfRegistration: true)
    SsoCodeService ssoCodeService = Mock()
    ManageUserService manageUserService = Mock()
    RegistrationService registrationService = Mock()

    @Subject
    SsoRegistrationController controller

    def setup() {
        controller = new SsoRegistrationController()
        setField(controller, "ssoProperties", ssoProperties)
        setField(controller, "ssoCodeService", ssoCodeService)
        setField(controller, "manageUserService", manageUserService)
        setField(controller, "registrationService", registrationService)
        ssoCodeService.readRegistrationTicket("the-ticket") >> "john@example.com"
    }

    def "should show the form with a suggested user name and the public projects"() {
        given:
        manageUserService.suggestUsername("john@example.com") >> "john"
        registrationService.getPublicProjects() >> [new PublicProjectDto(3, "Shop", null)]

        when:
        def response = controller.getRegistration(new SsoRegistrationReqDTO(ticket: "the-ticket"))

        then: "showing the form doesn't use up the ticket"
        0 * ssoCodeService.redeemRegistrationTicket(_)
        response.statusCode == HttpStatus.OK
        with(response.body) {
            email() == "john@example.com"
            username() == "john"
            projects()*.name() == ["Shop"]
        }
    }

    def "should create the account and tell the SUPER admins"() {
        given:
        def user = new User(username: "jkowalski", email: "john@example.com")

        when:
        def response = controller.register(new SsoRegistrationReqDTO(ticket: "the-ticket", username: "jkowalski",
                projectsIds: [3], message: "I test the shop"))

        then: "the name is checked, the ticket used up, the account created and the SUPER admins emailed, in this order"
        1 * manageUserService.checkNewSsoUser("john@example.com", "jkowalski") >> "jkowalski"
        then:
        1 * ssoCodeService.redeemRegistrationTicket("the-ticket") >> "john@example.com"
        then:
        1 * manageUserService.createSsoUser("john@example.com", "jkowalski") >> user
        then:
        1 * registrationService.notifySuperAdmins(user, [3], "I test the shop", "single sign-on")
        response.statusCode == HttpStatus.CREATED
        response.body.username == "jkowalski"
    }

    def "should keep the ticket when the user name is taken, so another one can be chosen"() {
        given:
        manageUserService.checkNewSsoUser("john@example.com", "john") >> { throw new ItemAlreadyExistsException(User, "john") }

        when:
        def response = controller.register(new SsoRegistrationReqDTO(ticket: "the-ticket", username: "john"))

        then:
        0 * ssoCodeService.redeemRegistrationTicket(_)
        0 * manageUserService.createSsoUser(*_)
        response.statusCode == HttpStatus.CONFLICT
        response.body.message == "The user name 'john' is already taken. Choose another one."
    }

    def "should refuse an expired or used ticket"() {
        given:
        ssoCodeService.readRegistrationTicket("old") >> { throw new IllegalArgumentException("Registration ticket already used") }

        when:
        def form = controller.getRegistration(new SsoRegistrationReqDTO(ticket: "old"))
        def created = controller.register(new SsoRegistrationReqDTO(ticket: "old", username: "john"))

        then:
        0 * manageUserService.createSsoUser(*_)
        form.statusCode == HttpStatus.UNAUTHORIZED
        created.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "should refuse when self-registration was switched off meanwhile"() {
        given:
        ssoProperties.selfRegistration = false

        when:
        def response = controller.register(new SsoRegistrationReqDTO(ticket: "the-ticket", username: "john"))

        then:
        0 * manageUserService.createSsoUser(*_)
        response.statusCode == HttpStatus.FORBIDDEN
    }

    private static void setField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }
}
