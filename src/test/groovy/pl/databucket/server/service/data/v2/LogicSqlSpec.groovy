package pl.databucket.server.service.data.v2

import pl.databucket.server.exception.InvalidRuleException
import spock.lang.Specification
import spock.lang.Unroll

class LogicSqlSpec extends Specification {

    Map<String, Object> params = [:]

    String sql(Object logic) {
        LogicSql.toSql(logic, "john", params, "lg")
    }

    def "should compare a field with a value as a parameter"() {
        expect:
        sql(["==": [[var: "tagId"], 3]]) == "(tag_id = :lg0)"
        params == [lg0: 3]
    }

    def "should compare a text property as text, with the path nodes as parameters"() {
        expect:
        sql(["==": [[var: '$.address.city'], "Paris"]]) == "((properties #>> ARRAY[CAST(:lg0 AS text), CAST(:lg1 AS text)]) = :lg2)"
        params == [lg0: "address", lg1: "city", lg2: "Paris"]
    }

    def "should decode the property names of the rich filter"() {
        expect:
        sql([">": [[var: 'prop.$*address*zip'], 10]]).contains("::numeric")
        params.lg0 == "address"
        params.lg1 == "zip"
        params.lg2 == 10G
    }

    @Unroll
    def "should cast a property to the type of the #type value it is compared with"() {
        expect:
        sql(["==": [[var: '$.a'], value]]).contains(cast)

        where:
        type      | value                       || cast
        "number"  | 5                           || "= 'number' THEN"
        "boolean" | true                        || "= 'boolean' THEN"
        "date"    | "2026-01-31T10:00:00.000Z"  || "::timestamp"
    }

    def "should swap the sides when the value is on the left"() {
        expect:
        sql(["<": [5, [var: "id"]]]) == "(data_id > :lg0)"
    }

    def "should translate a between"() {
        expect:
        sql(["<=": [1, [var: "id"], 9]]) == "((data_id >= :lg0) AND (data_id <= :lg1))"
        params == [lg0: 1, lg1: 9]
    }

    def "should combine and, or and not"() {
        expect:
        sql([and: [["==": [[var: "reserved"], false]], [or: [["==": [[var: "tagId"], 1]], ["!": ["==": [[var: "tagId"], 2]]]]]]]) ==
                "((reserved = :lg0) AND ((tag_id = :lg1) OR NOT ((tag_id = :lg2))))"
    }

    def "should check empty and not empty"() {
        expect:
        sql(["!": [var: "owner"]]) == "(reserved_by IS NULL)"
        sql(["!!": [var: '$.a']]).endsWith(" IS NOT NULL)")
    }

    def "should compare with null as IS NULL"() {
        expect:
        sql(["==": [[var: "owner"], null]]) == "(reserved_by IS NULL)"
        sql(["!=": [[var: "owner"], null]]) == "(reserved_by IS NOT NULL)"
    }

    def "should use the signed-in user for @currentUser"() {
        expect:
        sql(["==": [[var: "owner"], "@currentUser"]]) == "(reserved_by = :lg0)"
        params.lg0 == "john"
    }

    def "should find a field among values"() {
        expect:
        sql(["in": [[var: "tagId"], [1, 2]]]) == "(tag_id IN (:lg0))"
        params.lg0 == [1, 2]
        sql(["in": [[var: "tagId"], []]]) == "FALSE"
    }

    def "should find a property among values as jsonb"() {
        expect:
        sql(["in": [[var: '$.size'], ["S", "M"]]]) == "(CAST(:lg0 AS jsonb) @> (properties #> ARRAY[CAST(:lg1 AS text)]))"
        params.lg0 == '["S","M"]'
    }

    def "should find a value in a property: an item of a list or a part of a text, with LIKE characters escaped"() {
        when:
        def result = sql(["in": ["50%_", [var: '$.tags']]])

        then:
        result.contains("jsonb_typeof") && result.contains("@>") && result.contains(" LIKE ")
        params.values().contains('%50\\%\\_%')
        params.values().contains('["50%_"]')
    }

    @Unroll
    def "should translate the pattern operator #operator"() {
        expect:
        sql([(operator): [[var: '$.name'], "A%"]]).contains(" " + sqlOperator + " :")

        where:
        operator   || sqlOperator
        "like"     || "LIKE"
        "!like"    || "NOT LIKE"
        "similar"  || "SIMILAR TO"
        "!similar" || "NOT SIMILAR TO"
        "~"        || "~"
        "!~*"      || "!~*"
    }

    @Unroll
    def "should translate the property function #function"() {
        expect:
        sql(["==": [[var: '$.items.' + function], value]]).contains(part)

        where:
        function       | value || part
        "length()"     | 2     || "jsonb_array_length"
        "exists()"     | true  || "#> ARRAY[CAST(:lg0 AS text)]) IS NOT NULL"
        "isEmpty()"    | true  || "#>> ARRAY[CAST(:lg0 AS text)]) IS NULL"
    }

    def "should table-search the whole row as text"() {
        expect:
        sql(["like": [[var: "properties"], "%abc%"]]) == "((properties)::varchar LIKE :lg0)"
    }

    @Unroll
    def "should refuse #what"() {
        when:
        sql(logic)

        then:
        def e = thrown(InvalidRuleException)
        e.message.contains(message)

        where:
        what                                  | logic                                                    || message
        "an unknown field"                    | ["==": [[var: "password"], 1]]                           || "Unknown field"
        "SQL as a field"                      | ["==": [[var: "1=1; drop table users"], 1]]              || "Unknown field"
        "an unknown function"                 | ["==": [[var: '$.a.pg_sleep()'], 1]]                     || "Unknown property function"
        "an empty path node"                  | ["==": [[var: '$.a..b'], 1]]                             || "Invalid property path"
        "an unknown operator"                 | ["union": [[var: "id"], 1]]                              || "Unknown operator"
        "two values"                          | ["==": [1, 1]]                                           || "Both sides"
        "a list compared with =="             | ["==": [[var: "id"], [1, 2]]]                            || "single value"
        "an expression as an operand"         | ["==": [["+": [1, 2]], 3]]                               || "Expected a value"
    }

    def "should never put a value into the SQL text"() {
        given:
        def evil = "x'); DROP TABLE users; --"

        when:
        def result = sql([or: [
                ["==": [[var: '$.a'], evil]],
                ["like": [[var: "owner"], evil]],
                ["in": [evil, [var: '$.b']]],
                ["in": [[var: '$.c'], [evil]]],
                ["==": [[var: '$.' + "x'y"], 1]]]])

        then:
        !result.contains("DROP")
        !result.contains("x'y")
        params.values().any { it.toString().contains("DROP TABLE") }
    }

    def "should give the same parameter names when rendered again"() {
        given:
        def logic = ["==": [[var: '$.a'], "b"]]
        def first = LogicSql.toSql(logic, "john", params, "lg")
        def again = [:]

        expect:
        LogicSql.toSql(logic, "john", again, "lg") == first
        again == params
    }

    // ---- the history of a bucket

    String historySql(Object logic) {
        LogicSql.toSql(logic, "john", params, "hl", LogicSql.Fields.HISTORY)
    }

    def "should map the fields of the history entries"() {
        expect:
        historySql([and: [["==": [[var: "dataId"], 7]], ["==": [[var: "modifiedBy"], "@currentUser"]], [">": [[var: "id"], 3]]]]) ==
                "((data_id = :hl0) AND (modified_by = :hl1) AND (id > :hl2))"
        params == [hl0: 7, hl1: "john", hl2: 3]
    }

    def "should compare the time of a history entry as a timestamp"() {
        expect:
        historySql([">=": [[var: "modifiedAt"], "2026-11-09T10:00:00.000Z"]]) == "(modified_at >= :hl0)"
        params.hl0 instanceof java.sql.Timestamp
    }

    @Unroll
    def "should refuse #what in the history"() {
        when:
        historySql(logic)

        then:
        def e = thrown(InvalidRuleException)
        e.message.contains(message)

        where:
        what                   | logic                                              || message
        "a property"           | ["==": [[var: '$.status'], "used"]]                || "Properties can't be used here"
        "a property function"  | ["==": [[var: '$.tmp.exists()'], true]]           || "Properties can't be used here"
        "a field of the data"  | ["==": [[var: "owner"], "john"]]                   || "Unknown field: owner"
        "a column of the data" | ["==": [[var: "reserved_by"], "john"]]             || "Unknown field: reserved_by"
    }

    def "should keep the id of the data rows as data_id"() {
        expect:
        sql(["==": [[var: "id"], 1]]) == "(data_id = :lg0)"
    }
}
