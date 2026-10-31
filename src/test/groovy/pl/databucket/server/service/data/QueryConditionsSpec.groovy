package pl.databucket.server.service.data

import pl.databucket.server.dto.CustomColumnDto
import pl.databucket.server.exception.UnknownColumnException
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Conditions, columns and sorting: property paths and values come from the client, so they must reach
 * the database only as parameters, and column or function names only from a fixed list.
 */
class QueryConditionsSpec extends Specification {

    static final String INJECTION = "x}'; DROP TABLE users; --"
    static final String CITY_PATH = 'ARRAY[CAST(:path0_0 AS text), CAST(:path0_1 AS text)]'

    def "should pass a property path as parameters"() {
        given:
        def paramMap = [:]

        when:
        def sql = where(new Condition('$.address.city', Operator.equal, "Paris"), paramMap)

        then:
        sql == ' WHERE (properties #> ' + CITY_PATH + ')::text = :nr'
        paramMap == [path0_0: "address", path0_1: "city", nr: '"Paris"']
    }

    def "should not put an injected property path into the SQL"() {
        given:
        def paramMap = [:]

        when:
        def sql = where(new Condition('$.' + INJECTION, Operator.equal, 1), paramMap)

        then:
        !sql.contains("DROP")
        paramMap.path0_0 == INJECTION
    }

    def "should pass a list of values as one jsonb parameter"() {
        given:
        def paramMap = [:]

        when:
        def sql = where(new Condition('$.status', Operator.in, ["new", INJECTION]), paramMap)

        then:
        sql == ' WHERE CAST(:nr AS jsonb) @> ((properties #> ARRAY[CAST(:path0_0 AS text)]))'
        paramMap.nr == '["new","' + INJECTION + '"]'
    }

    def "should pass a value searched in a property array as a jsonb parameter"() {
        given:
        def paramMap = [:]

        when:
        def sql = where(new Condition(SourceType.s_const, INJECTION, Operator.in, SourceType.s_property, '$.tags'), paramMap)

        then:
        sql == " WHERE COALESCE(properties #> ARRAY[CAST(:path0_0 AS text)], '[]'::jsonb) @> (CAST(:nl AS jsonb))"
        paramMap.nl == '["' + INJECTION + '"]'
    }

    def "should compare a property with like as text"() {
        given:
        def paramMap = [:]

        expect:
        where(new Condition('$.address.city', Operator.like, "P%"), paramMap) == ' WHERE properties #>> ' + CITY_PATH + ' like :nr'
        paramMap.nr == "P%"
    }

    def "should pass the path of a property function as parameters"() {
        given:
        def paramMap = [:]

        expect:
        where(new Condition('$.tags.length()', Operator.greater, 2), paramMap) ==
                ' WHERE (coalesce(jsonb_array_length(properties #> ARRAY[CAST(:path0_0 AS text)]), 0)) > :nr'
        paramMap.path0_0 == "tags"
    }

    def "should accept the bucket table columns"() {
        expect:
        where(new Condition(COL.RESERVED_BY, Operator.equal, "john"), [:]) == ' WHERE reserved_by = :nr'
    }

    @Unroll
    def "should reject #description"() {
        when:
        where(condition, [:])

        then:
        thrown(UnknownColumnException)

        where:
        description                     | condition
        "an unknown column"             | new Condition(SourceType.s_field, "1=1 or true", Operator.equal, SourceType.s_const, 1)
        "a property without '\$.'"      | new Condition(SourceType.s_property, "1=1", Operator.equal, SourceType.s_const, 1)
        "an unknown property function"  | new Condition(SourceType.s_const, 1, Operator.equal, SourceType.s_function, '$.a.drop()')
        "an unknown column in like"     | new Condition(SourceType.s_field, "1=1 or true", Operator.like, SourceType.s_const, "a%")
        "an unknown column in in"       | new Condition(SourceType.s_field, "1=1 or true", Operator.in, SourceType.s_const, [1, 2])
    }

    def "should give each path its own parameters when queries share the parameters"() {
        given:
        def paramMap = [:]
        def condition = new Condition('$.name', Operator.equal, "John")

        when:
        def count = where(condition, paramMap)
        def data = where(condition, paramMap)

        then:
        count.contains(':path0_0')
        data.contains(':path1_0')
        paramMap.path0_0 == "name"
        paramMap.path1_0 == "name"
    }

    @Unroll
    def "should sort by '#sort'"() {
        given:
        def paramMap = [:]

        expect:
        new Query("x-bucket-1").select("*").from().orderBy(sort, paramMap).query == 'SELECT * FROM "x-bucket-1" ORDER BY ' + orderBy

        where:
        sort                   | orderBy
        "tagId"                | "tag_id"
        "desc(modifiedAt)"     | "modified_at desc"
        "reserved"             | "reserved"
        "desc(\$.address.city)" | "nullif(properties #> " + CITY_PATH + ", 'null'::jsonb) desc"
        "\$.address.city"       | "nullif(properties #> " + CITY_PATH + ", 'null'::jsonb)"
        "random"               | "RANDOM()"
    }

    @Unroll
    def "should reject sorting by '#sort'"() {
        when:
        new Query("x-bucket-1").select("*").from().orderBy(sort, [:])

        then:
        thrown(UnknownColumnException)

        where:
        sort << ["desc(1; DROP TABLE users)", "(select 1)", '$.a.drop()']
    }

    def "should select properties and columns given by the client"() {
        given:
        def paramMap = [:]
        def columns = [column('$.address.city', 'City" FROM users --'), column("tagId", null)]

        expect:
        new Query("x-bucket-1").selectData(columns, paramMap).query ==
                'SELECT properties #> ' + CITY_PATH + ' as "City"" FROM users --", tag_id'
    }

    def "should reject an unknown column selected by the client"() {
        when:
        new Query("x-bucket-1").selectData([column("1 as x; DROP TABLE users", null)], [:])

        then:
        thrown(UnknownColumnException)
    }

    private static String where(Condition condition, Map<String, Object> paramMap) {
        new Query("x-bucket-1").delete().where(condition, paramMap).query.substring("DELETE".length())
    }

    private static CustomColumnDto column(String field, String title) {
        new CustomColumnDto(field: field, title: title)
    }
}
