package pl.databucket.server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

@Configuration
@Order(2)
public class Oauth2SecurityConfig {

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Bean
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http,
        KeycloakLogoutHandler keycloakLogoutHandler) throws Exception {
        http.cors()
            .and()
            .csrf().disable()
            .authorizeRequests()
//            .antMatchers(
//                "/",
//                "/api/public/**", // public endpoint
//                "/**/static/**",
//                "/actuator/**",
//                "/**/favicon.ico",
//                "/login",
//                "/confirmation/**",
//                "/forgot-password",
//                "/sign-up",
//                "/change-password",
//                "/project",
//                "/project/**",
//                "/management",
//                "/management/**"
//            ).permitAll()
//            // swagger
//            .antMatchers(HttpMethod.GET,
//                "/swagger-ui/**",
//                "/v2/api-docs",
//                "/v3/api-docs",
//                "/webjars/**",            // swagger-ui webjars
//                "/swagger-resources/**",  // swagger-ui resources
//                "/configuration/**",      // swagger configuration
//                "/**/*.html",
//                "/**/*.css",
//                "/**/*.js"
//            ).permitAll()
            .antMatchers(HttpMethod.GET,
                "/", "/login**", "/index*", "/static/**", "/*.js", "/*.json", "/*.ico", "/api/public/auth-options")
            .permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2Login()
            .and()
            .logout()
            .addLogoutHandler(keycloakLogoutHandler)
            .logoutSuccessUrl("/");
//            .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
        http.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
        return http.build();
    }


}
