package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobKeywordRunDTO {

    @Schema(description = "The keyword of the jobs (Settings → Jobs), compared ignoring case", example = "DEV cleaning")
    private String keyword;
}
