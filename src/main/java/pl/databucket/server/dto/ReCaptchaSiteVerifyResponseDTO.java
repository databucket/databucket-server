package pl.databucket.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ReCaptchaSiteVerifyResponseDTO {

    private boolean success;
    private String hostname;

    @JsonProperty("challenge_ts")
    private Instant challengeTs;

    @JsonProperty("error-codes")
    private String[] errorCodes;

    private Float score;
    private String action;

}
