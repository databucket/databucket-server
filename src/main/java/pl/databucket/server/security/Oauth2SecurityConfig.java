package pl.databucket.server.security;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

@Log4j2

@Configuration
//@Order(2)
public class Oauth2SecurityConfig {

    @Bean
    public JwtOauthTokenFilter jwtOauthTokenFilter() {
        return new JwtOauthTokenFilter();
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
                "/", "/login**", "/index*", "/static/**", "/*.js", "/*.json", "/*.ico", "/api/auth/auth-options")
            .permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2Login()
            .defaultSuccessUrl("/login-callback")
            .and()
            .logout()
            .addLogoutHandler(keycloakLogoutHandler)
            .logoutSuccessUrl("/");
//            .and()
//            .exceptionHandling().authenticationEntryPoint(unauthorizedHandler);
//        http.oauth2ResourceServer().jwt();
//        http.addFilterBefore(jwtOauthTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


}
