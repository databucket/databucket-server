package pl.databucket.server.audit

import spock.lang.Specification

class AuditListenerLabelsSpec extends Specification {

    def "should name the linked objects, with the id only when a name repeats"() {
        expect:
        AuditListener.labelsOf([[4, "test"] as Object[], [20, "QA team"] as Object[], [22, "QA team"] as Object[], [7, null] as Object[]]) ==
                ["#7", "QA team #20", "QA team #22", "test"]
    }

    def "should give no labels for no links"() {
        expect:
        AuditListener.labelsOf([]) == []
    }
}
