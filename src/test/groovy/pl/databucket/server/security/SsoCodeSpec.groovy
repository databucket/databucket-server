package pl.databucket.server.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import spock.lang.Specification
import spock.lang.Subject

import java.lang.reflect.Field

class SsoCodeSpec extends Specification {

    @Subject
    TokenProvider tokenProvider

    def setup() {
        tokenProvider = new TokenProvider()
        setField(tokenProvider, "signingKey", "test-signing-key-that-is-long-enough-for-hs256")
        setField(tokenProvider, "expireHours", 1L)
    }

    def "should return the username an SSO code was issued for"() {
        when: "a code is issued and read back"
        def code = tokenProvider.generateSsoCode("john", 60)

        then:
        tokenProvider.getUsernameFromSsoCode(code) == "john"
    }

    def "should reject an expired SSO code"() {
        given: "a code that is already expired"
        def code = tokenProvider.generateSsoCode("john", -1)

        when:
        tokenProvider.getUsernameFromSsoCode(code)

        then:
        thrown(IllegalArgumentException)
    }

    def "should reject an SSO code signed with another key"() {
        given: "a code from an application with another secret"
        def other = new TokenProvider()
        setField(other, "signingKey", "another-signing-key-that-is-long-enough-for-hs256")
        def code = other.generateSsoCode("john", 60)

        when:
        tokenProvider.getUsernameFromSsoCode(code)

        then:
        thrown(IllegalArgumentException)
    }

    def "should not accept an access token as an SSO code"() {
        given: "an access token"
        def auth = new UsernamePasswordAuthenticationToken("john", null, [new SimpleGrantedAuthority("ROLE_MEMBER")])
        def accessToken = tokenProvider.generateToken(auth, 1)

        when:
        tokenProvider.getUsernameFromSsoCode(accessToken)

        then:
        thrown(IllegalArgumentException)
    }

    def "should not accept a confirmation link token as an SSO code"() {
        given: "a sign-up confirmation token"
        def confirmation = tokenProvider.packToJwts("john@example.com")

        when:
        tokenProvider.getUsernameFromSsoCode(confirmation)

        then:
        thrown(IllegalArgumentException)
    }

    def "should not accept an SSO code as an access token or a confirmation link"() {
        given:
        def code = tokenProvider.generateSsoCode("john", 60)

        expect: "the JWT filter finds no user in it and it carries no confirmation content"
        tokenProvider.getUsernameFromToken(code) == null
        tokenProvider.unpackFromJwts(code) == null
    }

    private static void setField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }
}
