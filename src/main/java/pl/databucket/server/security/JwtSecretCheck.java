package pl.databucket.server.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/**
 * The secret that signs the sign-in tokens (jwt.secret) must be the installation's own: whoever knows it can make a
 * token of any user, SUPER included. Databucket doesn't start without one, with a short one, or with one that has
 * been published (the default of the jar up to 3.20.1, the example of the documentation).
 */
public final class JwtSecretCheck {

    public static final int MIN_LENGTH = 32;
    static final String HOW = "Set the environment variable JWT_SECRET (or jwt.secret in your configuration file) to a "
            + "random text of at least " + MIN_LENGTH + " characters, e.g. the output of: openssl rand -base64 48. "
            + "Changing it signs out all users. See https://github.com/databucket/databucket-server/wiki/Installation#configuration";

    // SHA-256 of secrets that are public: the jwt.secret of application-default.yaml and application-testing.yaml
    // inside the jar up to 3.20.1, and the example of the wiki and the Docker Hub page
    private static final Set<String> PUBLIC = Set.of(
            "8c1f8d229e65e7ef2ffef4ed3d3c5ae7c131bf6921309021d62ee749979bc013",
            "e55b797ab1c03a76ffe1085a36749ae6fcd0b817fcf101ff12d5a88e35b73e38");

    private JwtSecretCheck() {
    }

    /** @throws IllegalStateException with what to do, when the secret can't be used */
    public static void verify(String secret) {
        if (secret == null || secret.isBlank())
            throw new IllegalStateException("jwt.secret is not set. " + HOW);
        if (secret.length() < MIN_LENGTH)
            throw new IllegalStateException("jwt.secret is too short (" + secret.length() + " characters). " + HOW);
        if (PUBLIC.contains(sha256(secret)))
            throw new IllegalStateException("jwt.secret is a published example or an old default of Databucket, "
                    + "so anyone could sign in as any user. " + HOW);
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
