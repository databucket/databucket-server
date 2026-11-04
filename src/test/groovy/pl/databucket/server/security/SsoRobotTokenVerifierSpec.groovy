package pl.databucket.server.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import pl.databucket.server.configuration.SsoProperties
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.reflect.Field
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey

/**
 * Checks of a robot's client token, with a local RSA key in place of the provider's JWKS.
 */
class SsoRobotTokenVerifierSpec extends Specification {

    static final String ISSUER = "https://idp.example.com/realms/databucket"

    static KeyPair providerKey = generateKey()
    static KeyPair otherKey = generateKey()

    SsoProperties ssoProperties = new SsoProperties(enabled: true, clientId: "databucket")

    @Subject
    SsoRobotTokenVerifier verifier

    def setup() {
        verifier = new SsoRobotTokenVerifier()
        setField(verifier, "ssoProperties", ssoProperties)
        def decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) providerKey.public).build()
        verifier.configureValidation(decoder, ISSUER)
        verifier.setDecoder(decoder)
    }

    def "should name the robot from a valid client token"() {
        expect:
        verifier.verify(token([:])) == "tests-robot"
    }

    def "should read the robot from the configured claim"() {
        given: "Okta puts the client ID into cid"
        ssoProperties.robotClaim = "cid"
        setup()

        expect:
        verifier.verify(token([cid: "okta-robot"])) == "okta-robot"
    }

    def "should accept the configured audience instead of the client ID"() {
        given:
        ssoProperties.robotAudience = "api://databucket"
        setup()

        expect:
        verifier.verify(token([aud: ["api://databucket"]])) == "tests-robot"
    }

    @Unroll
    def "should refuse a token #problem"() {
        when:
        verifier.verify(token(claims, key))

        then:
        thrown(JwtException)

        where:
        problem                              | claims                                                    | key
        "signed with another key"            | [:]                                                       | otherKey
        "that has expired"                   | [exp: new Date(System.currentTimeMillis() - 120_000)]     | providerKey
        "from another issuer"                | [iss: "https://evil.example.com"]                         | providerKey
        "issued for another application"     | [aud: ["account"]]                                        | providerKey
        "without audience"                   | [aud: []]                                                 | providerKey
        "without the robot claim"            | [azp: null]                                               | providerKey
    }

    def "should refuse a token that is not a JWT"() {
        when:
        verifier.verify("not-a-token")

        then:
        thrown(JwtException)
    }

    private static String token(Map overrides, KeyPair key = providerKey) {
        Map claims = [iss: ISSUER, aud: ["databucket"], azp: "tests-robot",
                      exp: new Date(System.currentTimeMillis() + 300_000)] + overrides
        def builder = new JWTClaimsSet.Builder()
        claims.each { name, value ->
            if (value != null)
                builder.claim(name, value)
        }
        def jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), builder.build())
        jwt.sign(new RSASSASigner(key.private))
        jwt.serialize()
    }

    private static KeyPair generateKey() {
        def generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        generator.generateKeyPair()
    }

    private static void setField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }
}
