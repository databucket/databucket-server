package pl.databucket.server.security

import pl.databucket.server.configuration.SsoProperties
import spock.lang.Specification
import spock.lang.Unroll

class SsoSelfRegistrationAllowedSpec extends Specification {

    @Unroll
    def "should #allowedText #email with self-registration #selfRegistration and domains #domains"() {
        given:
        def properties = new SsoProperties(enabled: true, selfRegistration: selfRegistration, selfRegistrationDomains: domains)

        expect:
        properties.isSelfRegistrationAllowed(email) == allowed

        where:
        email                     | selfRegistration | domains                         || allowed
        "john@example.com"        | false            | []                              || false
        "john@example.com"        | true             | []                              || true
        "john@example.com"        | true             | [""]                            || true
        "john@example.com"        | true             | ["example.com"]                 || true
        "john@example.com"        | true             | ["@Example.COM "]               || true
        "john@example.com"        | true             | ["company.com", "example.com"]  || true
        "john@example.com"        | true             | ["company.com"]                 || false
        "john@sub.example.com"    | true             | ["example.com"]                 || false
        "john@notexample.com"     | true             | ["example.com"]                 || false
        null                      | true             | []                              || false

        allowedText = allowed ? "allow" : "refuse"
    }

    def "should refuse self-registration when SSO is off"() {
        expect:
        !new SsoProperties(enabled: false, selfRegistration: true).isSelfRegistrationAllowed("john@example.com")
    }
}
