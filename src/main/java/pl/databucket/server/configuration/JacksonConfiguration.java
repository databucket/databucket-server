package pl.databucket.server.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Dates in the JSON of the API. They are written as before, {@code yyyy-MM-dd'T'HH:mm:ss.SSS'Z'} in UTC
 * ({@code spring.jackson.date-format}), but read as any ISO 8601 time stamp: with or without fractions of a second
 * (more digits than milliseconds are cut), with Z or an offset, or a date alone (midnight UTC); also milliseconds since
 * 1970. The date format alone refused "2030-01-01T00:00:00Z" and, being lenient, read ".123456789Z" as 123456789
 * milliseconds, moving the date by days without an error.
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer isoDates() {
        return builder -> builder.deserializerByType(Date.class, new IsoDateDeserializer());
    }

    public static class IsoDateDeserializer extends JsonDeserializer<Date> {

        @Override
        public Date deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT)
                return new Date(parser.getLongValue());
            String text = parser.getValueAsString();
            if (text == null || text.isBlank())
                return null;
            try {
                return Date.from(parse(text.trim()));
            } catch (DateTimeParseException e) {
                return (Date) context.handleWeirdStringValue(Date.class, text,
                        "not an ISO 8601 date and time, e.g. 2030-01-31T12:00:00Z or 2030-01-31T12:00:00.000+01:00");
            }
        }

        static Instant parse(String text) {
            if (text.length() == 10)
                return LocalDate.parse(text).atStartOfDay().toInstant(ZoneOffset.UTC);
            try {
                return OffsetDateTime.parse(text).toInstant();
            } catch (DateTimeParseException e) {
                // no zone: UTC, as the dates are written
                return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
            }
        }
    }
}
