package pl.databucket.server.audit

import pl.databucket.server.dto.TaskConfigDto
import spock.lang.Specification

import java.time.Instant

class AuditChangesSpec extends Specification {

    static AuditChanges.Property p(String name, Object before, Object after) {
        new AuditChanges.Property(name, before, after)
    }

    def "should record only the properties that changed, as [before, after]"() {
        expect:
        AuditChanges.of("Bucket", [p("name", "orders", "orders-v2"), p("history", false, true), p("description", "a", "a")]) ==
                [name: ["orders", "orders-v2"], history: [false, true]]
    }

    def "should record the values of a new object with before = null"() {
        expect:
        AuditChanges.of("Tag", [p("name", null, "used"), p("description", null, null)]) == [name: [null, "used"]]
    }

    def "should leave out the id, the project and the audit columns"() {
        expect:
        AuditChanges.of("Tag", [p("id", null, 5), p("projectId", null, 3), p("createdBy", null, "anna"),
                                p("modifiedAt", new Date(1), new Date(2)), p("name", null, "x")]) == [name: [null, "x"]]
    }

    def "should leave out the fields that change by themselves"() {
        expect:
        AuditChanges.of("TaskSchedule", [p("nextRunAt", Instant.EPOCH, Instant.now()), p("failures", 0, 1)]).isEmpty()
        AuditChanges.of("User", [p("lastSendEmailTempPasswordDate", null, new Date())]).isEmpty()
    }

    def "should mask secrets but show that they changed"() {
        expect:
        AuditChanges.of("User", [p("password", "old-hash", "new-hash"), p("clientSecret", null, "s3cr3t")]) ==
                [password: ["***", "***"], clientSecret: [null, "***"]]
    }

    def "should not mask a flag named like a secret"() {
        expect:
        AuditChanges.of("User", [p("changePassword", false, true)]) == [changePassword: [false, true]]
    }

    def "should leave out the deleted flag (the action says it)"() {
        expect:
        AuditChanges.of("Tag", [p("deleted", null, false), p("name", null, "x")]) == [name: [null, "x"]]
    }

    def "should write dates as ISO text and objects of jsonb columns as JSON"() {
        given:
        def config = new TaskConfigDto()
        config.actions = [type: "remove"]

        expect:
        AuditChanges.of("Task", [p("expirationDate", null, new Date(0)), p("configuration", null, config)]) ==
                [expirationDate: [null, "1970-01-01T00:00:00Z"], configuration: [null, [properties: null, actions: [type: "remove"]]]]
    }

    def "should compare references by their id and name"() {
        expect:
        AuditChanges.of("Bucket", [p("dataClass", [id: 1, name: "user"], [id: 1, name: "user"])]).isEmpty()
        AuditChanges.of("Bucket", [p("dataClass", [id: 1, name: "user"], [id: 2, name: "job"])]) ==
                [dataClass: [[id: 1, name: "user"], [id: 2, name: "job"]]]
    }

    def "should record a very long value only by its length"() {
        given:
        def svg = "x" * (AuditChanges.MAX_VALUE_LENGTH + 1)

        expect:
        AuditChanges.of("Svg", [p("structure", null, svg)]) == [structure: [null, "(" + svg.length() + " characters)"]]
    }
}
