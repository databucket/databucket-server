package pl.databucket.server.security

import jakarta.servlet.http.Cookie
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import spock.lang.Specification
import spock.lang.Subject

class SsoAuthorizationRequestCookieRepositorySpec extends Specification {

    static final String SECRET = "test-signing-key-that-is-long-enough-for-hs256"

    @Subject
    SsoAuthorizationRequestCookieRepository repository = new SsoAuthorizationRequestCookieRepository(SECRET)

    OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://idp.example.com/authorize")
            .clientId("databucket")
            .redirectUri("https://databucket.example.com/login/oauth2/code/oidc")
            .scopes(["openid", "email"] as Set)
            .state("state-123")
            .additionalParameters([nonce: "nonce-hash", code_challenge: "challenge", code_challenge_method: "S256"])
            .attributes([registration_id: "oidc", nonce: "nonce-1", code_verifier: "verifier-1"])
            .build()

    def "should restore the authorization request saved in the cookie"() {
        given: "the request was saved when redirecting to the IdP"
        def cookie = save(authorizationRequest)

        and: "the IdP calls back with the same state"
        def callback = callback("state-123", cookie)

        when:
        def loaded = repository.loadAuthorizationRequest(callback)

        then: "everything needed to finish the login is back"
        loaded.state == "state-123"
        loaded.clientId == "databucket"
        loaded.redirectUri == authorizationRequest.redirectUri
        loaded.authorizationUri == authorizationRequest.authorizationUri
        loaded.scopes == ["openid", "email"] as Set
        loaded.attributes == authorizationRequest.attributes
        loaded.additionalParameters == authorizationRequest.additionalParameters
    }

    def "should keep the cookie HttpOnly, Lax and scoped to the callback path"() {
        given:
        def request = new MockHttpServletRequest(contextPath: "/db")
        def response = new MockHttpServletResponse()

        when:
        repository.saveAuthorizationRequest(authorizationRequest, request, response)

        then:
        def header = response.getHeader("Set-Cookie")
        header.startsWith(SsoAuthorizationRequestCookieRepository.COOKIE_NAME + "=")
        header.contains("Path=/db/login/oauth2/")
        header.contains("HttpOnly")
        header.contains("SameSite=Lax")
        header.contains("Max-Age=600")
    }

    def "should ignore the cookie when the state does not match"() {
        given:
        def callback = callback("other-state", save(authorizationRequest))

        expect:
        repository.loadAuthorizationRequest(callback) == null
    }

    def "should ignore a tampered cookie"() {
        given: "the payload was changed but the signature kept"
        def cookie = save(authorizationRequest)
        def parts = cookie.split("\\.")
        def json = new String(Base64.urlDecoder.decode(parts[0]), "UTF-8").replace("databucket", "attacker")
        def tampered = Base64.urlEncoder.withoutPadding().encodeToString(json.getBytes("UTF-8")) + "." + parts[1]

        expect:
        repository.loadAuthorizationRequest(callback("state-123", tampered)) == null
    }

    def "should ignore a cookie signed by another application"() {
        given:
        def other = new SsoAuthorizationRequestCookieRepository("another-signing-key-that-is-long-enough-for-hs256")
        def request = new MockHttpServletRequest()
        def response = new MockHttpServletResponse()
        other.saveAuthorizationRequest(authorizationRequest, request, response)

        expect:
        repository.loadAuthorizationRequest(callback("state-123", response.getCookie(SsoAuthorizationRequestCookieRepository.COOKIE_NAME).value)) == null
    }

    def "should clear the cookie when the request is removed"() {
        given:
        def callback = callback("state-123", save(authorizationRequest))
        def response = new MockHttpServletResponse()

        when:
        def removed = repository.removeAuthorizationRequest(callback, response)

        then:
        removed.state == "state-123"
        response.getHeader("Set-Cookie").contains("Max-Age=0")
    }

    private String save(OAuth2AuthorizationRequest request) {
        def response = new MockHttpServletResponse()
        repository.saveAuthorizationRequest(request, new MockHttpServletRequest(), response)
        return response.getCookie(SsoAuthorizationRequestCookieRepository.COOKIE_NAME).value
    }

    private static MockHttpServletRequest callback(String state, String cookieValue) {
        def request = new MockHttpServletRequest()
        request.addParameter("state", state)
        request.setCookies(new Cookie(SsoAuthorizationRequestCookieRepository.COOKIE_NAME, cookieValue))
        return request
    }
}
