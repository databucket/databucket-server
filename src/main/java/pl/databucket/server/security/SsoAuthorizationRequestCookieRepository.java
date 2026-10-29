package pl.databucket.server.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Keeps the pending OIDC authorization request (state, nonce, PKCE verifier) in a signed cookie instead of
 * the HTTP session, so the IdP callback may land on any replica. The cookie is HttpOnly, short-lived and
 * scoped to the callback path; its HMAC is checked before anything in it is used.
 */
@Log4j2
public class SsoAuthorizationRequestCookieRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    static final String COOKIE_NAME = "DATABUCKET_SSO";
    private static final Duration MAX_AGE = Duration.ofMinutes(10);
    private static final String HMAC = "HmacSHA256";
    // keeps these signatures apart from anything else signed with the same secret
    private static final String SIGNATURE_CONTEXT = "sso-authorization-request:";

    private final ObjectMapper mapper = new ObjectMapper();
    private final byte[] key;

    public SsoAuthorizationRequestCookieRepository(String secret) {
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (state == null)
            return null;
        OAuth2AuthorizationRequest authorizationRequest = readCookie(request);
        return authorizationRequest != null && state.equals(authorizationRequest.getState()) ? authorizationRequest : null;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            setCookie(request, response, "", Duration.ZERO);
            return;
        }
        setCookie(request, response, sign(serialize(authorizationRequest)), MAX_AGE);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        setCookie(request, response, "", Duration.ZERO);
        return authorizationRequest;
    }

    private OAuth2AuthorizationRequest readCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .map(this::verify)
                .filter(Objects::nonNull)
                .map(this::deserialize)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void setCookie(HttpServletRequest request, HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .path(request.getContextPath() + "/login/oauth2/")
                .httpOnly(true)
                .secure(request.isSecure())
                // Lax: the IdP returns with a top-level GET, which still carries the cookie
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exp", Instant.now().plus(MAX_AGE).getEpochSecond());
        data.put("authorizationUri", authorizationRequest.getAuthorizationUri());
        data.put("clientId", authorizationRequest.getClientId());
        data.put("redirectUri", authorizationRequest.getRedirectUri());
        data.put("scopes", authorizationRequest.getScopes());
        data.put("state", authorizationRequest.getState());
        data.put("additionalParameters", authorizationRequest.getAdditionalParameters());
        data.put("attributes", authorizationRequest.getAttributes());
        data.put("authorizationRequestUri", authorizationRequest.getAuthorizationRequestUri());
        try {
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize the SSO authorization request", e);
        }
    }

    @SuppressWarnings("unchecked")
    private OAuth2AuthorizationRequest deserialize(String json) {
        try {
            Map<String, Object> data = mapper.readValue(json, new TypeReference<>() {});
            if (Instant.now().getEpochSecond() > ((Number) data.get("exp")).longValue())
                return null;
            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri((String) data.get("authorizationUri"))
                    .clientId((String) data.get("clientId"))
                    .redirectUri((String) data.get("redirectUri"))
                    .scopes(new LinkedHashSet<>((Collection<String>) data.get("scopes")))
                    .state((String) data.get("state"))
                    .additionalParameters((Map<String, Object>) data.get("additionalParameters"))
                    .attributes((Map<String, Object>) data.get("attributes"))
                    .authorizationRequestUri((String) data.get("authorizationRequestUri"))
                    .build();
        } catch (Exception e) {
            log.warn("Ignoring an unreadable SSO authorization request cookie: {}", e.getMessage());
            return null;
        }
    }

    String sign(String json) {
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return payload + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(payload));
    }

    /**
     * @return the signed JSON, or null when the value was not signed by this application
     */
    String verify(String value) {
        int dot = value.indexOf('.');
        if (dot < 0)
            return null;
        String payload = value.substring(0, dot);
        try {
            byte[] signature = Base64.getUrlDecoder().decode(value.substring(dot + 1));
            if (!MessageDigest.isEqual(hmac(payload), signature))
                return null;
            return new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(key, HMAC));
            return mac.doFinal((SIGNATURE_CONTEXT + payload).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
