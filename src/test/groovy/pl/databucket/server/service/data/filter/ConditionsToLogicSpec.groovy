package pl.databucket.server.service.data.filter

import pl.databucket.server.dto.DataGetDTO
import pl.databucket.server.exception.InvalidRuleException
import spock.lang.Specification
import spock.lang.Unroll

class ConditionsToLogicSpec extends Specification {

    @Unroll
    def "should convert #description"() {
        expect:
        ConditionsToLogic.convert([condition]) == [and: [logic]]

        where:
        description                  | condition                                                  || logic
        "a property and a value"     | c("property", '$.city', "=", "const", "Paris")             || ['==': [[var: '$.city'], 'Paris']]
        "a field"                    | c("field", "tagId", "<>", "const", 2)                      || ['!=': [[var: 'tagId'], 2]]
        "a column name"              | c("field", "data_id", "<", "const", 100)                   || ['<': [[var: 'data_id'], 100]]
        "a property function"        | c("function", '$.tags.length()', ">", "const", 2)          || ['>': [[var: '$.tags.length()'], 2]]
        "a value in a property"      | c("const", "red", "in", "property", '$.tags')              || ['in': ['red', [var: '$.tags']]]
        "a list"                     | c("field", "tagId", "in", "const", [1, 2])                 || ['in': [[var: 'tagId'], [1, 2]]]
        "a list as text of numbers"  | c("field", "tagId", "in", "const", "[1, 2]")               || ['in': [[var: 'tagId'], [1L, 2L]]]
        "a list as text"             | c("property", '$.s', "!in", "const", '"new", "done"')      || ['!in': [[var: '$.s'], ["new", "done"]]]
        "null"                       | c("field", "owner", "=", "const", null)                    || ['==': [[var: 'owner'], null]]
        "two properties"             | c("property", '$.a', "like", "property", '$.b')            || ['like': [[var: '$.a'], [var: '$.b']]]
    }

    @Unroll
    def "should refuse #description"() {
        when:
        ConditionsToLogic.convert([condition])

        then:
        def e = thrown(InvalidRuleException)
        e.message.contains(message)

        where:
        description                | condition                                                   || message
        "SQL as a function"        | c("function", "now()", ">", "field", "createdAt")           || "SQL text"
        "a property without \$."   | c("property", "1=1", "=", "const", 1)                       || "'\$.'"
        "an unknown source"        | c("sql", "1=1", "=", "const", 1)                            || "Unknown 'left_source'"
        "an unknown operator"      | c("field", "id", "between", "const", 1)                     || "Unknown operator"
        "no operator"              | [left_source: "field", left_value: "id"]                    || "needs an operator"
    }

    def "should translate the conditions of a request to SQL with parameters, combined with the rules"() {
        given:
        def filter = LogicFilter.of("john", new DataGetDTO(rules: [['$.a', '==', 'x']],
                conditions: [c("field", "owner", "=", "const", "@currentUser")]))
        def params = [:]

        when:
        def sql = filter.applyTo(new pl.databucket.server.service.data.Query("x-bucket-1").select("data_id").from(), params)
                .toString(org.slf4j.LoggerFactory.getLogger("test"), params)

        then:
        !filter.isEmpty()
        sql.contains("(reserved_by = :lg")
        params.containsValue("john")
    }

    def "should refuse a request with an unknown field in the conditions"() {
        when:
        LogicFilter.of("john", new DataGetDTO(conditions: [c("field", "1=1 or true", "=", "const", 1)]))

        then:
        def e = thrown(InvalidRuleException)
        e.message.contains("Unknown field")
    }

    private static Map<String, Object> c(String leftSource, Object leftValue, String operator, String rightSource, Object rightValue) {
        [left_source: leftSource, left_value: leftValue, operator: operator, right_source: rightSource, right_value: rightValue]
    }
}
