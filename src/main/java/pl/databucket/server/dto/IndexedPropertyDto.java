package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A property of the data rows of a bucket with a database index, so that rules on it are fast in a big bucket.
 * The path is written into the SQL of the index (and of the queries using it), so it allows only letters, digits,
 * '_', '-' and spaces in its nodes.
 */
@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndexedPropertyDto {

    public static final String TEXT = "text";
    public static final String NUMBER = "number";
    public static final String PATH_PATTERN = "\\$(\\.[\\p{L}\\p{N}_\\- ]+)+";

    @NotNull
    @Size(max = 100)
    @Pattern(regexp = PATH_PATTERN, message = "must be a property path like $.a.b (letters, digits, _, - and spaces)")
    @Schema(description = "The property", example = "$.account.status")
    private String path;

    @NotNull
    @Pattern(regexp = TEXT + "|" + NUMBER, message = "must be text or number")
    @Schema(description = "How the rules compare it: text (==, in, like 'A%') or number (==, <, >, between)", example = "text")
    private String type;
}
