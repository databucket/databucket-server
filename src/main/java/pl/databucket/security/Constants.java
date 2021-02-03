package pl.databucket.security;

public class Constants {

    public static final long ACCESS_TOKEN_VALIDITY_SECONDS = 12*60*60; // 12 hours
    public static final String SIGNING_KEY = "s-key";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String AUTHORITIES_KEY = "a-key";
    public static final String PROJECT_ID = "p-id";
}
