package pl.databucket.server.service.data

import pl.databucket.server.dto.DataModifyDTO
import spock.lang.Specification

/**
 * Removing and setting single properties: the JSON paths and values come from the client,
 * so they must reach the database only as parameters, never as SQL text.
 */
class QueryPropertiesSpec extends Specification {

    static final String INJECTION = "x}'; DROP TABLE users; --"

    def "should pass the paths to remove as parameters"() {
        given:
        def dto = new DataModifyDTO(propertiesToRemove: ['$.address.city', '$.' + INJECTION])
        def paramMap = [modified_by: "john"]

        when:
        def sql = update(dto, paramMap)

        then:
        sql == 'UPDATE "x-bucket-1" SET modified_by = :modified_by, properties = properties' +
                ' #- ARRAY[CAST(:propertyToRemove0_0 AS text), CAST(:propertyToRemove0_1 AS text)]' +
                ' #- ARRAY[CAST(:propertyToRemove1_0 AS text)]'
        paramMap.propertyToRemove0_0 == "address"
        paramMap.propertyToRemove0_1 == "city"
        paramMap.propertyToRemove1_0 == INJECTION
    }

    def "should pass the properties to set as one JSON parameter"() {
        given:
        def dto = new DataModifyDTO(propertiesToSet: ['$.address.city': "Paris", ('$.' + INJECTION): INJECTION])
        def paramMap = [modified_by: "john"]

        when:
        def sql = update(dto, paramMap)

        then:
        sql == 'UPDATE "x-bucket-1" SET modified_by = :modified_by, properties = jsonb_merge(properties, CAST(:propertiesToSet AS jsonb))'
        !sql.contains("DROP")
        paramMap.propertiesToSet.contains('"address":{"city":"Paris"}')
        paramMap.propertiesToSet.contains(INJECTION)
    }

    def "should remove and then set properties in one statement"() {
        given:
        def dto = new DataModifyDTO(propertiesToRemove: ['$.phone'], propertiesToSet: ['$.age': 35])
        def paramMap = [modified_by: "john"]

        expect:
        update(dto, paramMap).endsWith('properties = jsonb_merge(properties #- ARRAY[CAST(:propertyToRemove0_0 AS text)], CAST(:propertiesToSet AS jsonb))')
        paramMap.propertiesToSet == '{"age":35}'
    }

    def "should not touch the properties when they are replaced as a whole"() {
        given:
        def dto = new DataModifyDTO(propertiesToRemove: ['$.phone'])
        def paramMap = [modified_by: "john"]

        expect:
        new Query("x-bucket-1").update().set(paramMap).removeAndSetProperties(false, dto, paramMap).query ==
                'UPDATE "x-bucket-1" SET modified_by = :modified_by'
        paramMap.keySet() == ["modified_by"] as Set
    }

    private static String update(DataModifyDTO dto, Map<String, Object> paramMap) {
        new Query("x-bucket-1").update().set(paramMap).removeAndSetProperties(true, dto, paramMap).query
    }
}
