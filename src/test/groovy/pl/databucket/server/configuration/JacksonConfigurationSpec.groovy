package pl.databucket.server.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import spock.lang.Specification
import spock.lang.Unroll

import java.text.SimpleDateFormat

class JacksonConfigurationSpec extends Specification {

    static class Holder {
        public Date date
    }

    ObjectMapper mapper = mapper()

    private static ObjectMapper mapper() {
        def builder = new Jackson2ObjectMapperBuilder()
        // as spring.jackson.date-format in application.yaml
        def format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        format.setTimeZone(TimeZone.getTimeZone('UTC'))
        builder.dateFormat(format)
        new JacksonConfiguration().isoDates().customize(builder)
        builder.build()
    }

    @Unroll
    def "should read #text as #expected"() {
        when:
        def holder = mapper.readValue("{\"date\": ${text}}", Holder)

        then:
        holder.date.toInstant().toString() == expected

        where:
        text                                 | expected
        '"2030-01-01T00:00:00.123Z"'         | '2030-01-01T00:00:00.123Z'
        '"2030-01-01T00:00:00Z"'             | '2030-01-01T00:00:00Z'
        '"2030-01-01T00:00:00.123456789Z"'   | '2030-01-01T00:00:00.123Z'
        '"2030-01-01T02:00:00+02:00"'        | '2030-01-01T00:00:00Z'
        '"2030-01-01T00:00:00"'              | '2030-01-01T00:00:00Z'
        '"2030-01-01"'                       | '2030-01-01T00:00:00Z'
        '1893456000000'                      | '2030-01-01T00:00:00Z'
    }

    def "should read an empty text as no date"() {
        expect:
        mapper.readValue('{"date": ""}', Holder).date == null
        mapper.readValue('{"date": null}', Holder).date == null
    }

    @Unroll
    def "should refuse #text instead of guessing"() {
        when:
        mapper.readValue("{\"date\": \"${text}\"}", Holder)

        then:
        thrown(InvalidFormatException)

        where:
        text << ['yesterday', '2030-13-01T00:00:00Z', '01.01.2030']
    }

    def "should write dates as before"() {
        given:
        def holder = new Holder(date: Date.from(java.time.Instant.parse('2030-01-01T00:00:00Z')))

        expect:
        mapper.writeValueAsString(holder) == '{"date":"2030-01-01T00:00:00.000Z"}'
    }
}
