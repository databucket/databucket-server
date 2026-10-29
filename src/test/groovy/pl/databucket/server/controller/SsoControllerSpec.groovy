package pl.databucket.server.controller

import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import pl.databucket.server.configuration.SsoProperties
import pl.databucket.server.dto.AuthRespDTO
import pl.databucket.server.dto.SsoTokenReqDTO
import pl.databucket.server.entity.User
import pl.databucket.server.repository.UserRepository
import pl.databucket.server.security.AuthResponseBuilder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import pl.databucket.server.security.SsoClientRegistrationRepository
import pl.databucket.server.security.TokenProvider
import pl.databucket.server.service.UserService
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.reflect.Field

class SsoControllerSpec extends Specification {

    SsoProperties ssoProperties = new SsoProperties(enabled: true, provider: "keycloak", name: "Company SSO")
    ObjectProvider<SsoClientRegistrationRepository> registrationProvider = Mock()
    TokenProvider tokenProvider = Mock()
    UserRepository userRepository = Mock()
    UserService userService = Mock()
    AuthResponseBuilder authResponseBuilder = Mock()

    @Subject
    SsoController controller

    def setup() {
        controller = new SsoController()
        setField(controller, "ssoProperties", ssoProperties)
        setField(controller, "clientRegistrationRepository", registrationProvider)
        setField(controller, "tokenProvider", tokenProvider)
        setField(controller, "userRepository", userRepository)
        setField(controller, "userService", userService)
        setField(controller, "authResponseBuilder", authResponseBuilder)
    }

    def "should describe the SSO button"() {
        expect:
        with(controller.getConfig()) {
            enabled
            provider == "keycloak"
            name == "Company SSO"
        }
    }

    def "should hide the SSO button when SSO is disabled"() {
        given:
        ssoProperties.enabled = false

        expect:
        with(controller.getConfig()) {
            !enabled
            provider == null
        }
    }

    def "should send the browser back to the login page when the provider is not available"() {
        given: "SSO is enabled but the provider's configuration could not be loaded"
        def repository = Mock(SsoClientRegistrationRepository)
        repository.getRegistration() >> null
        registrationProvider.getIfAvailable() >> repository

        when:
        def response = controller.login(new MockHttpServletRequest(contextPath: "/db"))

        then:
        response.statusCode == HttpStatus.FOUND
        response.headers.getLocation().toString() == "/db/login?sso_error=sso_unavailable"
    }

    def "should log out of the provider when provider logout is on"() {
        given:
        ssoProperties.providerLogout = true
        registrationProvider.getIfAvailable() >> repositoryWith([end_session_endpoint: "https://idp/logout"])

        when:
        def response = controller.logout(request())

        then:
        response.statusCode == HttpStatus.FOUND
        response.headers.getLocation().toString() ==
                "https://idp/logout?client_id=databucket&post_logout_redirect_uri=https://db.example.com/db/login"
    }

    @Unroll
    def "should go straight to the login page when #reason"() {
        given:
        ssoProperties.providerLogout = providerLogout
        registrationProvider.getIfAvailable() >> repositoryWith(metadata)

        when:
        def response = controller.logout(request())

        then:
        response.statusCode == HttpStatus.FOUND
        response.headers.getLocation().toString() == "https://db.example.com/db/login"

        where:
        reason                                  | providerLogout | metadata
        "provider logout is off"                | false          | [end_session_endpoint: "https://idp/logout"]
        "the provider has no logout endpoint"   | true           | [:]
    }

    def "should exchange a valid code like a sign-in without password"() {
        given: "a valid code for an enabled user"
        def user = new User(username: "john", enabled: true)
        def details = Stub(UserDetails) {
            getUsername() >> "john"
            getAuthorities() >> []
        }
        tokenProvider.getUsernameFromSsoCode("the-code") >> "john"
        userRepository.findWithProjectsAndRolesByUsername("john") >> user
        userService.loadUserByUsername("john") >> details

        when:
        def response = controller.exchangeCode(new SsoTokenReqDTO(code: "the-code", projectId: 2))

        then: "the shared sign-in response is built for the chosen project, as an SSO login"
        1 * authResponseBuilder.build(user, { it.name == "john" }, 2, false, "auth.sso-signin") >> ResponseEntity.ok(new AuthRespDTO(token: "token"))
        response.statusCode == HttpStatus.OK
        response.body.token == "token"
    }

    def "should refuse an invalid or expired code"() {
        given:
        tokenProvider.getUsernameFromSsoCode("bad") >> { throw new IllegalArgumentException("Invalid SSO code") }

        when:
        def response = controller.exchangeCode(new SsoTokenReqDTO(code: "bad"))

        then:
        0 * authResponseBuilder.build(*_)
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "should refuse a user disabled after the code was issued"() {
        given:
        tokenProvider.getUsernameFromSsoCode("the-code") >> "john"
        userRepository.findWithProjectsAndRolesByUsername("john") >> new User(username: "john", enabled: false)

        when:
        def response = controller.exchangeCode(new SsoTokenReqDTO(code: "the-code"))

        then:
        0 * authResponseBuilder.build(*_)
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "should refuse to exchange codes when SSO is disabled"() {
        given:
        ssoProperties.enabled = false

        when:
        def response = controller.exchangeCode(new SsoTokenReqDTO(code: "the-code"))

        then:
        0 * tokenProvider.getUsernameFromSsoCode(_)
        response.statusCode == HttpStatus.NOT_FOUND
    }

    private SsoClientRegistrationRepository repositoryWith(Map<String, Object> providerMetadata) {
        def registration = ClientRegistration.withRegistrationId(SsoClientRegistrationRepository.REGISTRATION_ID)
                .clientId("databucket")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://idp/auth")
                .tokenUri("https://idp/token")
                .providerConfigurationMetadata(providerMetadata)
                .build()
        Stub(SsoClientRegistrationRepository) {
            getRegistration() >> registration
        }
    }

    private static MockHttpServletRequest request() {
        new MockHttpServletRequest(scheme: "https", serverName: "db.example.com", serverPort: 443, contextPath: "/db", requestURI: "/db/api/public/sso/logout")
    }

    private static void setField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }
}
