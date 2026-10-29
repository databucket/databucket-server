package pl.databucket.server.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import pl.databucket.server.configuration.SsoProperties;

/**
 * The OIDC login flow (/oauth2/authorization/oidc -> IdP -> /login/oauth2/code/oidc), only when sso.enabled.
 * Runs before the main chain and only on these paths; the API and form login stay as they are.
 */
@Configuration
@ConditionalOnProperty(prefix = "sso", name = "enabled", havingValue = "true")
public class SsoSecurityConfig {

    @Bean
    public SsoClientRegistrationRepository ssoClientRegistrationRepository(SsoProperties ssoProperties) {
        return new SsoClientRegistrationRepository(ssoProperties);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain ssoSecurityFilterChain(HttpSecurity http,
                                                      SsoClientRegistrationRepository clientRegistrationRepository,
                                                      SsoAuthenticationHandler ssoAuthenticationHandler,
                                                      @Value("${jwt.secret}") String secret) throws Exception {
        SsoAuthorizationRequestCookieRepository authorizationRequestRepository = new SsoAuthorizationRequestCookieRepository(secret);
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .clientRegistrationRepository(clientRegistrationRepository)
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestRepository(authorizationRequestRepository))
                        .successHandler(ssoAuthenticationHandler)
                        .failureHandler(ssoAuthenticationHandler)
                )
                // the login ends in a redirect with a code; nothing is kept in a session
                .securityContext(context -> context.securityContextRepository(new RequestAttributeSecurityContextRepository()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable());
        return http.build();
    }
}
