package pl.databucket.server.audit

import spock.lang.Specification
import spock.lang.Unroll

class AuditActionsSpec extends Specification {

    def "should read the details of an operation"() {
        expect:
        AuditActions.details("bucket={} count={}", ["orders", 12] as Object[]) == [bucket: "orders", count: 12]
        AuditActions.details("user={} reason={}", ["anna", "Bad credentials"] as Object[]) == [user: "anna", reason: "Bad credentials"]
        AuditActions.details("user={} reason=unknown user", ["bob"] as Object[]) == [user: "bob", reason: "unknown user"]
        AuditActions.details("{}reason=password sign-in disabled", ["user=anna "] as Object[]) == [user: "anna", reason: "password sign-in disabled"]
        AuditActions.details("user={} project={}", ["anna", null] as Object[]) == [user: "anna", project: null]
        AuditActions.details("id={} code={}", [5, "007"] as Object[]) == [id: 5, code: "007"]
    }

    @Unroll
    def "should record #action as #type"() {
        expect:
        AuditActions.of(action, message, args as Object[], "API")?.entityType() == type

        where:
        action                     | message                                       | args                         || type
        "auth.signin"              | "user={} project={}"                          | ["anna", 15]                 || "Account"
        "auth.signin-failed"       | "user={} reason={}"                           | ["anna", "Bad credentials"]  || "Account"
        "auth.sso-registration-offered" | "email={}"                               | ["a@b.c"]                    || null
        "data.create"              | "bucket={} count={}"                          | ["orders", 1000]             || "Data"
        "data.create"              | "bucket={} id={}"                             | ["orders", 7]                || null
        "data.modify"              | "bucket={} count={}"                          | ["orders", 3]                || null
        "data.reserve"             | "bucket={} count={} owner={}"                 | ["orders", 1, "anna"]        || null
        "data.delete"              | "bucket={} count={}"                          | ["orders", 3]                || "Data"
        "data.history-clear"       | "bucket={} count={}"                          | ["orders", 3]                || "Data"
        "task.run"                 | "id={} name={} bucket={} action={} count={}"  | [3, "trash", "orders", "modify", 5] || "Task"
        "task.keyword-run"         | "keyword={} jobs={} ok={} errors={}"          | ["DEV cleaning", 3, 2, 1]    || "Schedule"
        "project.clear"            | "id={} name={}"                               | [15, "Acme Shop"]            || "Project"
        "bucket.index-create"      | "table={} index={} path={} type={} ms={}"     | ["x-bucket-1", "i", '$.a', "text", 5] || "Bucket"
        "bucket.create"            | "id={} name={}"                               | [1, "orders"]                || null
        "tag.modify"               | "id={} name={}"                               | [1, "used"]                  || null
    }

    def "should leave the task runs of the scheduler to its own history"() {
        expect:
        AuditActions.of("task.run", "id={} name={}", [3, "trash"] as Object[], "SCHEDULER") == null
        AuditActions.of("task.run", "id={} name={}", [3, "trash"] as Object[], "WEB") != null
    }

    def "should name the object and the project of the operation"() {
        when:
        def signin = AuditActions.of("auth.signin", "user={} project={}", ["anna", 15] as Object[], "API")
        def clear = AuditActions.of("project.clear", "id={} name={}", [7, "Acme"] as Object[], "WEB")
        def data = AuditActions.of("data.delete", "bucket={} count={}", ["orders", 3] as Object[], "API")

        then:
        signin.entityName() == "anna"
        signin.projectId() == 15
        clear.entityName() == "Acme"
        clear.projectId() == 7
        data.entityName() == "orders"
        data.projectId() == null // the signed-in project, taken by the recorder
    }
}
