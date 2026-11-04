package pl.databucket.server.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import pl.databucket.server.configuration.SsoProperties;
import pl.databucket.server.entity.UsedSsoCode;
import pl.databucket.server.repository.UsedSsoCodeRepository;

import java.util.Date;

/**
 * Issues and redeems the single-use codes handed to the browser after an SSO login.
 * A code can be exchanged only once: it travels in a URL (/login/sso?code=...) and may end up in proxy logs.
 */
@Service
public class SsoCodeService {

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private SsoProperties ssoProperties;

    @Autowired
    private UsedSsoCodeRepository usedSsoCodeRepository;

    public String issue(String username) {
        return tokenProvider.generateSsoCode(username, ssoProperties.getCodeTtlSeconds());
    }

    /**
     * Marks the code as used and returns the user it was issued for.
     *
     * @throws IllegalArgumentException when the code is invalid, expired or has already been used
     */
    public String redeem(String code) {
        TokenProvider.SsoCode ssoCode = tokenProvider.parseSsoCode(code);
        markUsed(ssoCode.id(), ssoCode.expiresAt(), "SSO code already used");
        return ssoCode.username();
    }

    /** How long someone without an account has to fill in the registration after the SSO login. */
    public static final long REGISTRATION_TTL_SECONDS = 15 * 60;

    public String issueRegistrationTicket(String email) {
        return tokenProvider.generateSsoRegistrationTicket(email, REGISTRATION_TTL_SECONDS);
    }

    /**
     * Reads a registration ticket without using it up (to show the registration form).
     *
     * @return the email the ticket was issued for
     * @throws IllegalArgumentException when the ticket is invalid, expired or already used
     */
    public String readRegistrationTicket(String ticket) {
        TokenProvider.SsoRegistrationTicket parsed = tokenProvider.parseSsoRegistrationTicket(ticket);
        if (usedSsoCodeRepository.existsById(parsed.id()))
            throw new IllegalArgumentException("Registration ticket already used");
        return parsed.email();
    }

    /**
     * Uses up a registration ticket: the account is created once.
     *
     * @return the email the ticket was issued for
     * @throws IllegalArgumentException when the ticket is invalid, expired or already used
     */
    public String redeemRegistrationTicket(String ticket) {
        TokenProvider.SsoRegistrationTicket parsed = tokenProvider.parseSsoRegistrationTicket(ticket);
        markUsed(parsed.id(), parsed.expiresAt(), "Registration ticket already used");
        return parsed.email();
    }

    private void markUsed(String id, Date expiresAt, String usedMessage) {
        usedSsoCodeRepository.deleteExpired(new Date());
        try {
            // no surrounding transaction: a duplicate key fails this insert only
            usedSsoCodeRepository.saveAndFlush(new UsedSsoCode(id, expiresAt));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(usedMessage);
        }
    }
}
