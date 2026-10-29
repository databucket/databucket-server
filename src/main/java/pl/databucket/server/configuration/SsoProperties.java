package pl.databucket.server.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Single sign-on through any OpenID Connect provider (Entra ID, Google, Okta, Keycloak, ...).
 * SSO only authenticates: the user must already exist in Databucket with the same email,
 * roles and projects are still managed in Databucket.
 */
@Component
@ConfigurationProperties(prefix = "sso")
@Getter
@Setter
public class SsoProperties {

    private boolean enabled = false;

    /** Selects the button style on the login page: microsoft, google, okta, keycloak or generic. */
    private String provider = "generic";

    /** Button label suffix ("Sign in with ..."); when empty the provider's name is used. */
    private String name;

    /** OIDC issuer, e.g. https://login.microsoftonline.com/{tenant}/v2.0 or https://host/realms/{realm}. */
    private String issuerUri;

    private String clientId;

    private String clientSecret;

    private List<String> scopes = List.of("openid", "email", "profile");

    /** Claim with the email matched against Databucket users. */
    private String emailClaim = "email";

    /** How long the code handed to the browser after the IdP login can be exchanged for a token. */
    private long codeTtlSeconds = 120;

    /**
     * Whether logging out of Databucket after an SSO login also ends the session at the provider
     * (through its end_session_endpoint). Off by default: with Microsoft or Google it signs the user
     * out of their whole account, not only Databucket.
     */
    private boolean providerLogout = false;
}
