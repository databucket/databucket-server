package pl.databucket.server.security;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import pl.databucket.server.configuration.SsoProperties;

/**
 * The single "oidc" client registration, discovered from the issuer on first use instead of at startup,
 * so Databucket starts (and form login works) even when the identity provider is unreachable.
 * A failed discovery is retried on the next login.
 */
@Log4j2
public class SsoClientRegistrationRepository implements ClientRegistrationRepository {

    public static final String REGISTRATION_ID = "oidc";

    private final SsoProperties properties;
    private volatile ClientRegistration registration;

    public SsoClientRegistrationRepository(SsoProperties properties) {
        this.properties = properties;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return REGISTRATION_ID.equals(registrationId) ? getRegistration() : null;
    }

    /**
     * @return the registration, or null when the provider's configuration can't be fetched
     */
    public ClientRegistration getRegistration() {
        if (registration == null) {
            synchronized (this) {
                if (registration == null) {
                    try {
                        registration = ClientRegistrations.fromIssuerLocation(properties.getIssuerUri())
                                .registrationId(REGISTRATION_ID)
                                .clientId(properties.getClientId())
                                .clientSecret(properties.getClientSecret())
                                .scope(properties.getScopes())
                                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                                .build();
                    } catch (RuntimeException e) {
                        log.error("Cannot load the SSO provider configuration from {}: {}", properties.getIssuerUri(), e.getMessage());
                    }
                }
            }
        }
        return registration;
    }
}
