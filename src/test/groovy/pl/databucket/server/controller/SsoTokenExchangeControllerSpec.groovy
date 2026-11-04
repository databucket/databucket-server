package pl.databucket.server.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.jwt.BadJwtException
import pl.databucket.server.configuration.SsoProperties
import pl.databucket.server.dto.AuthRespDTO
import pl.databucket.server.dto.SsoTokenExchangeReqDTO
import pl.databucket.server.entity.Role
import pl.databucket.server.entity.User
import pl.databucket.server.repository.UserRepository
import pl.databucket.server.security.AuthResponseBuilder
import pl.databucket.server.security.SsoRobotTokenVerifier
import pl.databucket.server.service.UserService
import spock.lang.Specification
import spock.lang.Subject

import java.lang.reflect.Field

class SsoTokenExchangeControllerSpec extends Specification {

    SsoProperties ssoProperties = new SsoProperties(enabled: true)
    SsoRobotTokenVerifier verifier = Mock()
    UserRepository userRepository = Mock()
    UserService userService = Mock()
    AuthResponseBuilder authResponseBuilder = Mock()

    @Subject
    SsoTokenExchangeController controller

    def setup() {
        controller = new SsoTokenExchangeController()
        setField(controller, "ssoProperties", ssoProperties)
        setField(controller, "robotTokenVerifier", verifier)
        setField(controller, "userRepository", userRepository)
        setField(controller, "userService", userService)
        setField(controller, "authResponseBuilder", authResponseBuilder)
    }

    def "should sign in a robot like a sign-in without password"() {
        given: "a valid client token of an enabled robot"
        def robot = user("tests-robot", "ROBOT")
        verifier.verify("client-token") >> "tests-robot"
        userRepository.findWithProjectsAndRolesByUsername("tests-robot") >> robot
        userService.loadUserByUsername("tests-robot") >> Stub(UserDetails) {
            getUsername() >> "tests-robot"
            getAuthorities() >> []
        }

        when:
        def response = controller.exchangeToken(new SsoTokenExchangeReqDTO(token: "client-token", projectId: 3))

        then: "the shared sign-in response is built for the chosen project"
        1 * authResponseBuilder.build(robot, { it.name == "tests-robot" }, 3, false, "auth.robot-signin") >> ResponseEntity.ok(new AuthRespDTO(token: "token"))
        response.statusCode == HttpStatus.OK
        response.body.token == "token"
    }

    def "should refuse a client token of a user without the ROBOT role"() {
        given:
        verifier.verify("client-token") >> "john"
        userRepository.findWithProjectsAndRolesByUsername("john") >> user("john", "MEMBER")

        when:
        def response = controller.exchangeToken(new SsoTokenExchangeReqDTO(token: "client-token"))

        then:
        0 * authResponseBuilder.build(*_)
        response.statusCode == HttpStatus.FORBIDDEN
    }

    def "should refuse a robot that is #reason"() {
        given:
        verifier.verify("client-token") >> "tests-robot"
        userRepository.findWithProjectsAndRolesByUsername("tests-robot") >> found

        when:
        def response = controller.exchangeToken(new SsoTokenExchangeReqDTO(token: "client-token"))

        then:
        0 * authResponseBuilder.build(*_)
        response.statusCode == HttpStatus.UNAUTHORIZED

        where:
        reason          | found
        "unknown"       | null
        "disabled"      | user("tests-robot", "ROBOT", false)
    }

    def "should refuse an invalid client token"() {
        given:
        verifier.verify("bad") >> { throw new BadJwtException("The token is not issued for databucket") }

        when:
        def response = controller.exchangeToken(new SsoTokenExchangeReqDTO(token: "bad"))

        then:
        0 * userRepository.findWithProjectsAndRolesByUsername(_)
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "should answer 503 when the provider can't be reached"() {
        given:
        verifier.verify("client-token") >> { throw new SsoRobotTokenVerifier.ProviderUnavailableException("Single sign-on is not available at the moment.") }

        when:
        def response = controller.exchangeToken(new SsoTokenExchangeReqDTO(token: "client-token"))

        then:
        response.statusCode == HttpStatus.SERVICE_UNAVAILABLE
    }

    def "should answer 400 without a token"() {
        when:
        def response = controller.exchangeToken(new SsoTokenExchangeReqDTO(token: " "))

        then:
        0 * verifier.verify(_)
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "should refuse client tokens when SSO is disabled"() {
        given:
        ssoProperties.enabled = false

        when:
        def response = controller.exchangeToken(new SsoTokenExchangeReqDTO(token: "client-token"))

        then:
        0 * verifier.verify(_)
        response.statusCode == HttpStatus.NOT_FOUND
    }

    private static User user(String name, String role, boolean enabled = true) {
        new User(username: name, enabled: enabled, roles: [new Role(name: role)] as Set)
    }

    private static void setField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }
}
