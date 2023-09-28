package pl.databucket.server.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Log4j2
@Component
@RequiredArgsConstructor
public class OAuth2LogoutHandler implements LogoutHandler {

    private final RestTemplate restTemplate;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
        Authentication auth) {
        logoutFromOAuth2((OidcUser) auth.getPrincipal());
    }

    private void logoutFromOAuth2(OidcUser user) {
        String endSessionEndpoint = user.getIssuer() + "/protocol/openid-connect/logout";
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(endSessionEndpoint)
            .queryParam("id_token_hint", user.getIdToken().getTokenValue());

        ResponseEntity<String> logoutResponse = restTemplate.getForEntity(builder.toUriString(), String.class);
        if (logoutResponse.getStatusCode().is2xxSuccessful()) {
            log.info("Successfully logged out from OAuth2");
        } else {
            log.error("Could not propagate logout to OAuth2");
        }
    }

}
