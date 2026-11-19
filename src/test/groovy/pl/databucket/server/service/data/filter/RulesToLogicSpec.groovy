package pl.databucket.server.service.data.filter

import pl.databucket.server.dto.DataGetDTO
import pl.databucket.server.exception.InvalidRuleException
import pl.databucket.server.service.data.Operator
import pl.databucket.server.service.data.Query
import spock.lang.Specification
import spock.lang.Unroll

class RulesToLogicSpec extends Specification {

    def "should convert rules to JsonLogic"() {
        expect:
        RulesToLogic.convert([['$.city', '=', 'Paris'], [or: [['tagId', 'in', [1, 2]], ['$.age', '>', 30]]]]) ==
                [and: [['==': [[var: '$.city'], 'Paris']],
                       [or: [['in': [[var: 'tagId'], [1, 2]]], ['>': [[var: '$.age'], 30]]]]]]
    }

    @Unroll
    def "should map the rule operator #rule to #logic"() {
        expect:
        RulesToLogic.convert([['$.a', rule, 1]]) == [and: [[(logic): [[var: '$.a'], 1]]]]

        where:
        rule      || logic
        "="       || "=="
        "=="      || "=="
        "<>"      || "!="
        "!="      || "!="
        "is not"  || "is not"
        "LIKE"    || "like"
        "!~*"     || "!~*"
    }

    def "should convert a negation"() {
        expect:
        RulesToLogic.convert([["!": ['$.a', "==", 1]]]) == [and: [["!": ["==": [[var: '$.a'], 1]]]]]
        RulesToLogic.convert([["!": [and: [['$.a', "==", 1]]]]]) == [and: [["!": [and: [["==": [[var: '$.a'], 1]]]]]]]
    }

    @Unroll
    def "should refuse #what in version 2"() {
        when:
        RulesToLogic.convert(rules)

        then:
        def e = thrown(InvalidRuleException)
        e.message.contains(message)

        where:
        what                    | rules                                      || message
        "a rule as SQL text"    | ["(properties->'a' is not null) = true"]   || "SQL text"
        "the #func: prefix"     | [["#func:now()", ">", "x"]]                || "SQL text"
        "an unknown operator"   | [['$.a', 'between', 1]]                    || "Unknown operator"
        "a rule of two items"   | [['$.a', '==']]                            || "3 items"
    }

    def "should combine logic, rules and the server's conditions with AND"() {
        given:
        def filter = LogicFilter.of("john", new DataGetDTO(logic: ["==": [[var: "tagId"], 1]], rules: [['$.a', '==', 'x']]))
        filter.require("reserved_by", Operator.equal, "john")
        def params = [:]

        when:
        def sql = filter.applyTo(new Query("x-bucket-1").select("data_id").from(), params).toString(org.slf4j.LoggerFactory.getLogger("test"), params)

        then:
        sql.contains("WHERE ((tag_id = :lg0) AND ((")
        sql.endsWith("AND (reserved_by = :rq0)")
        params.rq0 == "john"
    }

    def "should read the field names of the history in the history rules"() {
        expect:
        RulesToLogic.convert([['dataId', '==', 5], ['modifiedBy', '==', 'owner']], LogicSql.Fields.HISTORY.names()) ==
                [and: [["==": [[var: 'dataId'], 5]], ["==": [[var: 'modifiedBy'], 'owner']]]]
    }
}
