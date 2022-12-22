package pl.databucket.server.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AppProperties {

    @Value("${spring.mail.from}")
    private String mailFrom;

    @Value("${recaptcha.enabled}")
    private Boolean recaptchaEnabled;

    @Value("${recaptcha.siteKey}")
    private String recaptchaSiteKey;

    @Value("${recaptcha.secretKey}")
    private String recaptchaSecretKey;

}
