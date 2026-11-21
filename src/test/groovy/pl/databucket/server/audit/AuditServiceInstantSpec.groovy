package pl.databucket.server.audit

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

class AuditServiceInstantSpec extends Specification {

    @Unroll
    def "should read '#text' as now shifted by #shift"() {
        when:
        Instant before = Instant.now()
        Instant result = AuditService.instant(text)
        Instant after = Instant.now()

        then:
        !result.isBefore(before.plus(shift))
        !result.isAfter(after.plus(shift))

        where:
        text       || shift
        "@now"     || Duration.ZERO
        "@now-1d"  || Duration.ofDays(-1)
        "@now-2h"  || Duration.ofHours(-2)
        "@now+30m" || Duration.ofMinutes(30)
        "@now-1w"  || Duration.ofDays(-7)
    }

    def "should read an ISO moment and nothing for blank"() {
        expect:
        AuditService.instant("2026-11-21T10:00:00Z") == Instant.parse("2026-11-21T10:00:00Z")
        AuditService.instant(null) == null
        AuditService.instant(" ") == null
    }

    def "should refuse other text"() {
        when:
        AuditService.instant("yesterday")

        then:
        thrown(DateTimeParseException)
    }
}
