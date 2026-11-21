package pl.databucket.server.security

import spock.lang.Specification
import spock.lang.Unroll

class JwtSecretCheckSpec extends Specification {

    def "should accept an own secret of at least 32 characters"() {
        when:
        JwtSecretCheck.verify('x' * 32)
        JwtSecretCheck.verify('k8Jd0t4mWq9vZr2pL7sYc1nB6hF3gX5eA0uE8iO2')

        then:
        noExceptionThrown()
    }

    @Unroll
    def "should refuse a secret that is #what"() {
        when:
        JwtSecretCheck.verify(secret)

        then:
        def e = thrown(IllegalStateException)
        e.message.startsWith(start)
        e.message.contains('JWT_SECRET')
        e.message.contains('openssl rand -base64 48')

        where:
        what                     | secret                                                                   | start
        'missing'                | null                                                                     | 'jwt.secret is not set'
        'empty'                  | ''                                                                       | 'jwt.secret is not set'
        'blank'                  | '   '                                                                    | 'jwt.secret is not set'
        'too short'              | 'veryverysecret'                                                         | 'jwt.secret is too short (14 characters)'
        'the old default'        | 'SGVsbG9Yb3JsZFRoaXNJc015U2VjcmV0S2V5Rm9xSldUVG9rZW5zMTIzNDU2Nzg5MA==' | 'jwt.secret is a published example'
        'the documented example' | 'change-me-to-a-random-text-of-at-least-32-characters'                   | 'jwt.secret is a published example'
    }
}
