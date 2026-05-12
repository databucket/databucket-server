package pl.databucket.server.service

import org.postgresql.util.PGobject
import spock.lang.Specification
import spock.lang.Unroll

import java.sql.Timestamp

class ServiceUtilsSpec extends Specification {

    ServiceUtils serviceUtils = new ServiceUtils()

    def "should convert Timestamp to ISO string"() {
        given: "a map with Timestamp value"
        def timestamp = Timestamp.valueOf("2024-01-15 10:30:45")
        def data = [[id: 1, createdAt: timestamp]]

        when: "converting properties columns"
        serviceUtils.convertPropertiesColumns(data)

        then: "timestamp is converted to ISO string"
        data[0].createdAt instanceof String
        (data[0].createdAt as String).contains("2024-01-15")
    }

    @Unroll
    def "should convert PGobject string '#pgValue' to proper type"() {
        given: "a map with PGobject value"
        def pgObject = new PGobject(type: "jsonb", value: pgValue)
        def data = [[id: 1, field: pgObject]]

        when: "converting properties columns"
        serviceUtils.convertPropertiesColumns(data)

        then: "PGobject is converted to expected type"
        data[0].field == expectedValue
        data[0].field?.class == expectedType

        where: "testing different PGobject values"
        pgValue      || expectedValue | expectedType
        '"text"'     || "text"        | String
        'true'       || true          | Boolean
        'false'      || false         | Boolean
        'null'       || null          | null
        '42'         || 42            | Integer
        '3.14'       || 3.14          | Double
        '123'        || 123           | Integer
    }

    def "should handle quoted strings in PGobject"() {
        given: "a PGobject with quoted string"
        def pgObject = new PGobject(type: "jsonb", value: '"hello world"')
        def data = [[message: pgObject]]

        when:
        serviceUtils.convertPropertiesColumns(data)

        then: "quotes are removed"
        data[0].message == "hello world"
    }

    def "should handle multiple entries with different types"() {
        given: "multiple maps with different value types"
        def timestamp = Timestamp.valueOf("2024-01-15 10:30:45")
        def pgString = new PGobject(type: "jsonb", value: '"test"')
        def pgNumber = new PGobject(type: "jsonb", value: '100')
        def pgBool = new PGobject(type: "jsonb", value: 'true')

        def data = [
                [id: 1, createdAt: timestamp, status: pgString],
                [id: 2, count: pgNumber, active: pgBool]
        ]

        when:
        serviceUtils.convertPropertiesColumns(data)

        then: "all conversions work correctly"
        data[0].createdAt instanceof String
        (data[0].status as String).contains("test")
        data[1].count == 100
        data[1].active == true
    }

    def "should handle null values without errors"() {
        given: "a map with null values"
        def data = [[id: 1, field: null, other: null]]

        when:
        serviceUtils.convertPropertiesColumns(data)

        then: "null values remain null"
        data[0].field == null
        data[0].other == null
        noExceptionThrown()
    }

    def "should handle empty list"() {
        given: "an empty list"
        def data = []

        when:
        serviceUtils.convertPropertiesColumns(data)

        then: "no errors occur"
        data.isEmpty()
        noExceptionThrown()
    }

    def "should preserve non-Timestamp and non-PGobject values"() {
        given: "a map with regular values"
        def data = [[id: 1, name: "John", age: 30, active: true]]

        when:
        serviceUtils.convertPropertiesColumns(data)

        then: "values remain unchanged"
        data[0].id == 1
        data[0].name == "John"
        data[0].age == 30
        data[0].active == true
    }

    def "should handle JSON object strings in PGobject"() {
        given: "a PGobject with JSON object"
        def jsonObject = '{"key": "value", "number": 123}'
        def pgObject = new PGobject(type: "jsonb", value: jsonObject)
        def data = [[config: pgObject]]

        when:
        serviceUtils.convertPropertiesColumns(data)

        then: "JSON object is preserved as string"
        data[0].config == jsonObject
    }
}