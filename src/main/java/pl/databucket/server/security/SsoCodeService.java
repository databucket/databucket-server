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
        usedSsoCodeRepository.deleteExpired(new Date());
        try {
            // no surrounding transaction: a duplicate key fails this insert only
            usedSsoCodeRepository.saveAndFlush(new UsedSsoCode(ssoCode.id(), ssoCode.expiresAt()));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("SSO code already used");
        }
        return ssoCode.username();
    }
}
