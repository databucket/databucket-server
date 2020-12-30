package pl.databucket.security;

public class Constants {

    public static final long ACCESS_TOKEN_VALIDITY_SECONDS = 24*60*60; // 24 hours
    public static final String SIGNING_KEY = "very-secure-singing-key";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String AUTHORITIES_KEY = "very-secure-authorities-key";
}
