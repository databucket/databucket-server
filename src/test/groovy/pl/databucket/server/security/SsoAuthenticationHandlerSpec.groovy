package pl.databucket.server.security

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import pl.databucket.server.configuration.SsoProperties
import pl.databucket.server.entity.User
import pl.databucket.server.repository.UserRepository
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.reflect.Field

class SsoAuthenticationHandlerSpec extends Specification {

    UserRepository userRepository = Mock()
    TokenProvider tokenProvider = Mock()
    SsoProperties ssoProperties = new SsoProperties()

    @Subject
    SsoAuthenticationHandler handler

    MockHttpServletRequest request = new MockHttpServletRequest(contextPath: "/db")
    MockHttpServletResponse response = new MockHttpServletResponse()

    def setup() {
        handler = new SsoAuthenticationHandler()
        setField(handler, "userRepository", userRepository)
        setField(handler, "tokenProvider", tokenProvider)
        setField(handler, "ssoProperties", ssoProperties)
    }

    def "should send a known user to the login page with a code"() {
        given: "an enabled user with the email from the IdP, in another case"
        userRepository.findByEmailIgnoreCase("john@example.com") >> new User(username: "john", email: "john@example.com", enabled: true)

        when:
        handler.onAuthenticationSuccess(request, response, login([sub: "1", email: "John@Example.com", email_verified: true]))

        then: "a code for that user is issued and handed over in the redirect"
        1 * tokenProvider.generateSsoCode("john", 120) >> "the-code"
        response.redirectedUrl == "/db/login/sso?code=the-code"
    }

    @Unroll
    def "should refuse with '#reason' when #situation"() {
        given:
        userRepository.findByEmailIgnoreCase("john@example.com") >> user

        when:
        handler.onAuthenticationSuccess(request, response, login(attributes))

        then: "no code is issued and the login page shows the reason"
        0 * tokenProvider.generateSsoCode(*_)
        response.redirectedUrl == "/db/login?sso_error=" + reason

        where:
        situation                     | attributes                                                     | user                                                   || reason
        "the IdP sends no email"      | [sub: "1"]                                                     | null                                                   || SsoAuthenticationHandler.NO_EMAIL
        "the email is not verified"   | [sub: "1", email: "john@example.com", email_verified: false]   | new User(username: "john", enabled: true)              || SsoAuthenticationHandler.EMAIL_NOT_VERIFIED
        "no user has the email"       | [sub: "1", email: "john@example.com"]                          | null                                                   || SsoAuthenticationHandler.UNKNOWN_USER
        "the user is disabled"        | [sub: "1", email: "john@example.com"]                          | new User(username: "john", enabled: false)             || SsoAuthenticationHandler.USER_DISABLED
    }

    def "should match by a configured claim other than email"() {
        given: "the IdP puts the email into upn and does not send email_verified for it"
        ssoProperties.emailClaim = "upn"
        userRepository.findByEmailIgnoreCase("john@example.com") >> new User(username: "john", enabled: true)

        when:
        handler.onAuthenticationSuccess(request, response, login([sub: "1", upn: "john@example.com", email_verified: false]))

        then:
        1 * tokenProvider.generateSsoCode("john", 120) >> "the-code"
        response.redirectedUrl == "/db/login/sso?code=the-code"
    }

    def "should send the user back to the login page when the IdP login fails"() {
        when:
        handler.onAuthenticationFailure(request, response, new OAuth2AuthenticationException(new OAuth2Error("invalid_token")))

        then:
        response.redirectedUrl == "/db/login?sso_error=" + SsoAuthenticationHandler.IDP_ERROR
    }

    def "should not keep a session after the login"() {
        given:
        def session = request.getSession(true)
        userRepository.findByEmailIgnoreCase(_) >> null

        when:
        handler.onAuthenticationSuccess(request, response, login([sub: "1", email: "john@example.com"]))

        then:
        session.invalid
    }

    private static OAuth2AuthenticationToken login(Map<String, Object> attributes) {
        def user = new DefaultOAuth2User([new SimpleGrantedAuthority("OIDC_USER")], attributes, "sub")
        return new OAuth2AuthenticationToken(user, user.authorities, "oidc")
    }

    private static void setField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }
}
