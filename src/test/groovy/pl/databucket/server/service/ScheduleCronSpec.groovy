package pl.databucket.server.service

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

class ScheduleCronSpec extends Specification {

    @Unroll
    def "should store the cron '#cron' as '#stored'"() {
        expect:
        ScheduleCron.normalize(cron) == stored

        where:
        cron                || stored
        "0 2 * * *"         || "0 0 2 * * *"
        " */15  * * * * "   || "0 */15 * * * *"
        "30 0 2 * * MON-FRI" || "30 0 2 * * MON-FRI"
    }

    @Unroll
    def "should refuse the cron '#cron'"() {
        when:
        ScheduleCron.normalize(cron)

        then:
        def e = thrown(ScheduleCron.InvalidCronException)
        e.message.contains(message)

        where:
        cron            || message
        null            || "missing"
        "  "            || "missing"
        "* * *"         || "3 fields"
        "0 99 * * *"    || "invalid"
        "* * * * * *"   || "more often than once a minute"
        "*/30 * * * * *" || "more often than once a minute"
    }

    def "should give the next run in the time zone of the schedule"() {
        given:
        def after = Instant.parse("2026-11-21T12:00:00Z")

        expect: '2:00 in Warsaw (UTC+1 in November) is 1:00 UTC'
        ScheduleCron.next("0 0 2 * * *", "Europe/Warsaw", after) == Instant.parse("2026-11-22T01:00:00Z")
        ScheduleCron.next("0 0 2 * * *", "UTC", after) == Instant.parse("2026-11-22T02:00:00Z")
    }

    def "should check the time zone"() {
        expect:
        ScheduleCron.zone(null).id == "UTC"
        ScheduleCron.zone("Europe/Warsaw").id == "Europe/Warsaw"

        when:
        ScheduleCron.zone("Mars/Base")

        then:
        thrown(ScheduleCron.InvalidCronException)
    }
}
