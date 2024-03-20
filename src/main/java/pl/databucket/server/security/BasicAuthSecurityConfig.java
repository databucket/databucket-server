package pl.databucket.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pl.databucket.server.dto.AuthRespDTO;

@Log4j2
@Configuration
@Profile("!oauth2")
public class BasicAuthSecurityConfig {

    @Bean
    public SecurityFilterChain basicSecurityFilterChain(HttpSecurity http,
        AuthResponseBuilder authResponseBuilder,
        JsonAuthenticationFilter jsonLoginFilter,
        ObjectMapper mapper) throws Exception {
        AuthenticationSuccessHandler successHandler = getFormSuccessHandler(authResponseBuilder);
        AuthenticationFailureHandler failureHandler = getAuthenticationFailureHandler(mapper);
        http.cors().and().csrf().disable()
            .addFilterBefore(jsonLoginFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers(HttpMethod.GET,
                "/", "/login**", "/sign-up", "/forgot-password", "/change-password",
                "/index*", "/**/static/**", "/*.js", "/*.json", "/**/*.ico",
                "/api/auth/**")
            .permitAll()
            .antMatchers(HttpMethod.POST,
                "/api/auth/sign-up", "/api/auth/forgot-password"
            )
            .permitAll()
            .anyRequest()
            .authenticated()
            .and()
            .formLogin()
            .loginPage("/login-form")
            .successHandler(successHandler)
            .failureHandler(failureHandler)
            .permitAll();
//            .and()
//            .exceptionHandling().authenticationEntryPoint(unauthorizedHandler);
        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.oauth2ResourceServer().jwt();
        return http.build();
    }

    @Bean
    public AuthenticationFailureHandler getAuthenticationFailureHandler(ObjectMapper mapper) {
        return (request, response, exception) -> {
            log.error("Auth error", exception);
            AuthRespDTO authResponse = AuthRespDTO.builder().message(exception.getMessage()).build();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write(mapper.writeValueAsString(authResponse));
        };
    }

    @Bean
    public AuthenticationSuccessHandler getFormSuccessHandler(AuthResponseBuilder authResponseBuilder) {
        return new FormAuthSuccessHandler(authResponseBuilder);
    }
}
