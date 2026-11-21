package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Schema(description = "Generating random data rows on the server: a generator per property")
public class DataGenerateDTO {

    @Schema(description = "Property path -> generator. A text with #{...} is a Datafaker expression (providers such as "
            + "Name, Address, Internet, PhoneNumber, Company, Commerce, Finance, Lorem; numerify ###, letterify ???, "
            + "regexify); {n} in a text is the number of the row (unique); {\"number\": [min, max], \"decimals\": 2}; "
            + "{\"oneOf\": [values], \"weights\": [numbers]}; {\"date\": [from, to]} (yyyy-MM-dd); "
            + "{\"datetime\": [from, to]} (ISO 8601, UTC); any other text, number, boolean or null is the same in every row.",
            example = "{\"$.firstName\": \"#{Name.firstName}\", \"$.email\": \"user-{n}@example.com\", "
                    + "\"$.age\": {\"number\": [18, 90]}, \"$.status\": {\"oneOf\": [\"active\", \"blocked\"], "
                    + "\"weights\": [9, 1]}, \"$.birthDate\": {\"date\": [\"1950-01-01\", \"2005-12-31\"]}}")
    private Map<String, Object> fields;

    @Schema(description = "Number of rows, 1..10000 (a preview returns at most 10)", example = "100")
    private Integer count;

    @Schema(description = "Tag id of the rows", example = "1")
    private Long tagId;

    @Schema(description = "True reserves the rows for the signed-in user", example = "false")
    private Boolean reserved;

    @Schema(description = "Language of the values, e.g. en (default), pl, de, fr-FR", example = "en")
    private String locale;

    @Schema(description = "Seed: the same seed and fields give the same rows again", example = "42")
    private Long seed;

    @Schema(description = "Number {n} of the first row. Default: after the last id of the bucket, so {n} stays unique "
            + "between calls", example = "1")
    private Long startN;

}
