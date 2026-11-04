package pl.databucket.server.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import pl.databucket.server.configuration.SsoProperties;

/**
 * Verifies a client token (client credentials grant) a robot got from the SSO provider and tells which robot it is.
 * The token must be signed by the provider (its JWKS), come from the configured issuer, be issued for Databucket
 * (audience) and not be expired.
 */
@Component
public class SsoRobotTokenVerifier {

    /** The provider is not configured or can't be reached. */
    public static class ProviderUnavailableException extends RuntimeException {
        public ProviderUnavailableException(String message) {
            super(message);
        }
    }

    @Autowired
    private SsoProperties ssoProperties;

    @Autowired
    private ObjectProvider<SsoClientRegistrationRepository> clientRegistrationRepository;

    private volatile JwtDecoder decoder;

    /**
     * @return the robot's name: the value of sso.robot-claim
     * @throws JwtException                 when the token is invalid, expired, from another issuer or for another audience
     * @throws ProviderUnavailableException when the provider's configuration can't be loaded
     */
    public String verify(String token) {
        Jwt jwt = getDecoder().decode(token);
        String robot = jwt.getClaimAsString(ssoProperties.getRobotClaim());
        if (robot == null || robot.isBlank())
            throw new BadJwtException("The token has no " + ssoProperties.getRobotClaim() + " claim");
        return robot;
    }

    String expectedAudience() {
        String audience = ssoProperties.getRobotAudience();
        return audience != null && !audience.isBlank() ? audience : ssoProperties.getClientId();
    }

    private JwtDecoder getDecoder() {
        if (decoder == null) {
            synchronized (this) {
                if (decoder == null)
                    decoder = createDecoder();
            }
        }
        return decoder;
    }

    private JwtDecoder createDecoder() {
        SsoClientRegistrationRepository repository = clientRegistrationRepository.getIfAvailable();
        ClientRegistration registration = repository != null ? repository.getRegistration() : null;
        if (registration == null || registration.getProviderDetails().getJwkSetUri() == null)
            throw new ProviderUnavailableException("Single sign-on is not available at the moment.");

        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withJwkSetUri(registration.getProviderDetails().getJwkSetUri()).build();
        configureValidation(nimbusDecoder, registration.getProviderDetails().getIssuerUri());
        return nimbusDecoder;
    }

    /** Checks of a signed token: expiry, issuer and audience. Package-private for tests (with a local key). */
    void configureValidation(NimbusJwtDecoder nimbusDecoder, String issuer) {
        String audience = expectedAudience();
        nimbusDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                        "The token is not issued for " + audience + " (aud: " + jwt.getAudience() + ")", null))));
    }

    /** For tests: use this decoder instead of the provider's. */
    void setDecoder(JwtDecoder decoder) {
        this.decoder = decoder;
    }
}
