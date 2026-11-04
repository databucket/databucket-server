package pl.databucket.server.controller

import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import pl.databucket.server.configuration.AppProperties
import pl.databucket.server.configuration.SsoProperties
import pl.databucket.server.dto.AuthReqDTO
import pl.databucket.server.dto.ForgotPasswordReqDTO
import pl.databucket.server.dto.SignUpDtoRequest
import pl.databucket.server.repository.UserRepository
import pl.databucket.server.service.ManageUserService
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.reflect.Field

/**
 * sso.password-login: false - the server refuses every use of a password.
 */
class PasswordLoginDisabledSpec extends Specification {

    SsoProperties ssoProperties = new SsoProperties(enabled: true, passwordLogin: false)
    AuthenticationManager authenticationManager = Mock()
    UserRepository userRepository = Mock()
    ManageUserService manageUserService = Mock()
    AppProperties appProperties = Mock()

    @Subject
    PublicController controller

    def setup() {
        controller = new PublicController()
        setField(controller, "ssoProperties", ssoProperties)
        setField(controller, "authenticationManager", authenticationManager)
        setField(controller, "userRepository", userRepository)
        setField(controller, "manageUserService", manageUserService)
        setField(controller, "appProperties", appProperties)
    }

    @Unroll
    def "should refuse #action when password sign-in is disabled"() {
        when:
        def response = call(controller)

        then: "nothing is checked, sent or created"
        0 * userRepository._
        0 * authenticationManager._
        0 * manageUserService._
        response.statusCode == HttpStatus.FORBIDDEN
        response.body.message == "Password sign-in is disabled. Use single sign-on."

        where:
        action                                 | call
        "the sign-in"                          | { PublicController c -> c.signIn(new AuthReqDTO(username: "super", password: "secret")) }
        "the forgotten password email"         | { PublicController c -> c.forgotPassword(new ForgotPasswordReqDTO("john@example.com", "http://localhost")) }
        "the forgotten password link"          | { PublicController c -> c.forgotPasswordConfirmation("link-token") }
        "the sign-up"                          | { PublicController c -> c.signUp(new SignUpDtoRequest(username: "john", password: "secret")) }
        "the sign-up confirmation link"        | { PublicController c -> c.signUpConfirmation("link-token") }
    }

    def "should keep the password sign-in when SSO is off, whatever password-login says"() {
        given:
        ssoProperties.enabled = false

        when:
        controller.signIn(new AuthReqDTO(username: "super", password: "secret"))

        then: "the user is looked up as usual"
        1 * userRepository.findWithProjectsAndRolesByUsername("super")
    }

    def "should keep the password sign-in by default"() {
        expect:
        new SsoProperties(enabled: true).passwordLogin
        !new SsoProperties(enabled: true).passwordLoginDisabled
    }

    private static void setField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }
}
