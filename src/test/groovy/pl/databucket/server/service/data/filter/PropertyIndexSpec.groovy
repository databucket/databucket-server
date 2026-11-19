package pl.databucket.server.service.data.filter

import pl.databucket.server.dto.IndexedPropertyDto
import pl.databucket.server.service.BucketIndexService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Indexed properties: the index and the rules on the property must give the same SQL expression, otherwise
 * PostgreSQL doesn't use the index.
 */
class PropertyIndexSpec extends Specification {

    static final String CITY = "ARRAY['address', 'city']"
    Map<String, Object> params = [:]

    def "should create a text index with text_pattern_ops and a number index on the expression of the rules"() {
        expect:
        PropertyIndex.createSql("x-bucket-7", new IndexedPropertyDto('$.address.city', "text"), true) ==
                'CREATE INDEX CONCURRENTLY IF NOT EXISTS "' + PropertyIndex.indexName("x-bucket-7", new IndexedPropertyDto('$.address.city', "text")) +
                '" ON "x-bucket-7" ((properties #>> ' + CITY + ') text_pattern_ops)'
        PropertyIndex.createSql("x-bucket-7", new IndexedPropertyDto('$.age', "number"), false) ==~
                /CREATE INDEX IF NOT EXISTS "x-bucket-7-p-[0-9a-f]{10}" ON "x-bucket-7" \(\(CASE WHEN jsonb_typeof\(\(properties #> ARRAY\['age'\]\)\) = 'number' THEN \(\(properties #> ARRAY\['age'\]\)\)::numeric END\)\)/
    }

    def "should name the index by the table, the type and the path"() {
        given:
        def text = PropertyIndex.indexName("x-bucket-7", new IndexedPropertyDto('$.a', "text"))

        expect:
        text.startsWith("x-bucket-7-p-")
        text == PropertyIndex.indexName("x-bucket-7", new IndexedPropertyDto('$.a', "text"))
        text != PropertyIndex.indexName("x-bucket-7", new IndexedPropertyDto('$.a', "number"))
        text != PropertyIndex.indexName("x-bucket-7", new IndexedPropertyDto('$.b', "text"))
    }

    def "should write an indexed path as a literal and compare it as the text index does"() {
        expect:
        LogicSql.toSql(["==": [[var: '$.address.city'], "Paris"]], "john", params, "lg", ['$.address.city': "text"]) ==
                "((properties #>> " + CITY + ") = :lg0)"
        params == [lg0: "Paris"]
    }

    def "should compare an indexed number property with the expression of the number index"() {
        expect:
        LogicSql.toSql(["<=": [10, [var: '$.age'], 20]], "john", params, "lg", ['$.age': "number"]) ==
                "(((CASE WHEN jsonb_typeof((properties #> ARRAY['age'])) = 'number' THEN ((properties #> ARRAY['age']))::numeric END) >= :lg0)" +
                " AND ((CASE WHEN jsonb_typeof((properties #> ARRAY['age'])) = 'number' THEN ((properties #> ARRAY['age']))::numeric END) <= :lg1))"
    }

    def "should compare 'in' of an indexed text property as text, and other properties as before"() {
        expect:
        LogicSql.toSql(["in": [[var: '$.status'], ["NEW", "PAID"]]], "john", params, "lg", ['$.status': "text"]) ==
                "((properties #>> ARRAY['status']) IN (:lg0) AND jsonb_typeof((properties #> ARRAY['status'])) = 'string')"
        LogicSql.toSql(["in": [[var: '$.other'], ["NEW"]]], "john", [:], "lg", ['$.status': "text"]).contains("@>")
        LogicSql.toSql(["in": [[var: '$.status'], [1, 2]]], "john", [:], "lg", ['$.status': "text"]).contains("@>")
    }

    def "should keep the path of a property without an index as parameters"() {
        expect:
        LogicSql.toSql(["==": [[var: '$.name'], "x"]], "john", params, "lg", ['$.status': "text"]).contains("CAST(:lg0 AS text)")
    }

    def "should double the quotes of a node written as a literal"() {
        expect:
        PropertyIndex.pathLiteral('$.a.b c') == "ARRAY['a', 'b c']"
    }

    @Unroll
    def "should refuse the indexed properties #description"() {
        when:
        BucketIndexService.check(properties)

        then:
        thrown(IllegalArgumentException)

        where:
        description               | properties
        "with SQL in the path"    | [new IndexedPropertyDto('$.a\'); DROP TABLE users; --', "text")]
        "without \$."             | [new IndexedPropertyDto('name', "text")]
        "with an empty node"      | [new IndexedPropertyDto('$.a..b', "text")]
        "of an unknown type"      | [new IndexedPropertyDto('$.a', "date")]
        "with a path twice"       | [new IndexedPropertyDto('$.a', "text"), new IndexedPropertyDto('$.a', "number")]
        "more than 5"             | (1..6).collect { new IndexedPropertyDto('$.p' + it, "text") }
    }

    def "should accept letters of any language, digits, _, - and spaces"() {
        when:
        BucketIndexService.check([new IndexedPropertyDto('$.adres.miejscowość', "text"), new IndexedPropertyDto('$.order-no.line_2 x', "number")])

        then:
        noExceptionThrown()
    }
}
