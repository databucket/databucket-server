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

    /**
     * Whether users can still sign in with a user name and password while SSO is enabled. When false the server
     * refuses every password sign-in (all roles, also SUPER and ROBOT), sign-up and forgotten password;
     * robots sign in with a client token from the provider instead (POST /api/public/sso/token-exchange).
     * Only applies when SSO is enabled.
     */
    private boolean passwordLogin = true;

    /**
     * Claim of a client token (client credentials grant) that names the robot; its value must equal the user name
     * of a Databucket user with the ROBOT role. azp works for Keycloak and Entra ID; for Okta use cid.
     */
    private String robotClaim = "azp";

    /** Audience a robot's client token must be issued for; when empty, the client-id. */
    private String robotAudience;

    /** True when SSO is on and password sign-in is switched off. */
    public boolean isPasswordLoginDisabled() {
        return enabled && !passwordLogin;
    }
}
