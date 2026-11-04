package pl.databucket.server.security

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import pl.databucket.server.configuration.TestSecurityConfig
import pl.databucket.server.entity.UsedSsoCode
import pl.databucket.server.repository.UsedSsoCodeRepository
import spock.lang.Specification
import spock.lang.Subject

/**
 * Single use of SSO codes on a real database (H2). Not @Transactional on purpose:
 * the second use has to fail on the primary key of a row committed by the first one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestSecurityConfig)
class SsoCodeServiceIntegrationSpec extends Specification {

    @Subject
    @Autowired
    SsoCodeService ssoCodeService

    @Autowired
    UsedSsoCodeRepository usedSsoCodeRepository

    def cleanup() {
        usedSsoCodeRepository.deleteAll()
    }

    def "should exchange a code only once"() {
        given:
        def code = ssoCodeService.issue("john")

        expect: "the first exchange gives the user"
        ssoCodeService.redeem(code) == "john"

        when: "the same code is sent again"
        ssoCodeService.redeem(code)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "SSO code already used"
    }

    def "should accept a new code for the same user"() {
        given: "the first code was used"
        ssoCodeService.redeem(ssoCodeService.issue("john"))

        expect: "the code issued for the project choice works"
        ssoCodeService.redeem(ssoCodeService.issue("john")) == "john"
    }

    def "should remove used codes once they have expired"() {
        given: "a used code that expired a minute ago"
        usedSsoCodeRepository.saveAndFlush(new UsedSsoCode("expired-code", new Date(System.currentTimeMillis() - 60_000)))

        when: "another code is exchanged"
        ssoCodeService.redeem(ssoCodeService.issue("john"))

        then: "only the fresh code is kept"
        usedSsoCodeRepository.findAll()*.id.every { it != "expired-code" }
        usedSsoCodeRepository.count() == 1
    }
}
