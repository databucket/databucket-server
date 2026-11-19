package pl.databucket.server.service.data

import pl.databucket.server.dto.CustomColumnDto
import pl.databucket.server.exception.UnknownColumnException
import spock.lang.Specification
import spock.lang.Unroll

/**
 * The server's own conditions, columns and sorting: property paths and values come from the client, so they must
 * reach the database only as parameters, and column or function names only from a fixed list. The filters of the
 * client are tested in filter/LogicSqlSpec.
 */
class QuerySpec extends Specification {

    static final String CITY_PATH = 'ARRAY[CAST(:path0_0 AS text), CAST(:path0_1 AS text)]'

    def "should combine the server's conditions with AND, values as parameters"() {
        given:
        def paramMap = [:]
        def conditions = new LinkedHashMap<String, Object>()
        conditions.put(COL.DATA_ID, [1L, 2L])
        conditions.put(COL.RESERVED_BY, "john")

        expect:
        where(conditions, paramMap) == ' WHERE data_id IN (:w0) AND reserved_by = :w1'
        paramMap == [w0: [1L, 2L], w1: "john"]
    }

    def "should match nothing for an empty list and test null with IS NULL"() {
        given:
        def conditions = new LinkedHashMap<String, Object>()
        conditions.put(COL.DATA_ID, [])
        conditions.put(COL.RESERVED_BY, null)

        expect:
        where(conditions, [:]) == ' WHERE FALSE AND reserved_by IS NULL'
    }

    def "should reject a column outside the bucket table"() {
        when:
        where([("1=1 or true"): 1], [:])

        then:
        thrown(UnknownColumnException)
    }

    @Unroll
    def "should sort by '#sort'"() {
        given:
        def paramMap = [:]

        expect:
        new Query("x-bucket-1").select("*").from().orderBy(sort, paramMap).query == 'SELECT * FROM "x-bucket-1" ORDER BY ' + orderBy

        where:
        sort                    | orderBy
        "tagId"                 | "tag_id"
        "desc(modifiedAt)"      | "modified_at desc"
        "reserved"              | "reserved"
        "desc(\$.address.city)" | "nullif(properties #> " + CITY_PATH + ", 'null'::jsonb) desc"
        "\$.address.city"       | "nullif(properties #> " + CITY_PATH + ", 'null'::jsonb)"
        "random"                | "RANDOM()"
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

    private static String where(Map<String, Object> conditions, Map<String, Object> paramMap) {
        new Query("x-bucket-1").delete().where(conditions, paramMap).query.substring("DELETE".length())
    }

    private static CustomColumnDto column(String field, String title) {
        new CustomColumnDto(field: field, title: title)
    }
}
