package pl.databucket.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import pl.databucket.server.dto.AuthRespDTO;

@Log4j2
@Configuration
public class OAuth2SecurityConfig {

    @Bean
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http,
        OAuth2LogoutHandler oauth2LogoutHandler,
        TokenProvider tokenUtils,
        ModelMapper modelMapper,
        ObjectMapper mapper) throws Exception {
        AuthenticationSuccessHandler successHandler = getSuccessHandler(tokenUtils, modelMapper);
        AuthenticationFailureHandler failureHandler = getAuthenticationFailureHandler(mapper);
        http.cors().and().csrf().disable()
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
            .and()
            .formLogin()
            .loginPage("/login-form")
            .successHandler(successHandler)
            .failureHandler(failureHandler)
            .permitAll()
            .and()
            .oauth2Login()
            .defaultSuccessUrl("/login-callback")
            .successHandler(successHandler)
            .failureHandler(failureHandler)
            .and()
            .logout()
            .addLogoutHandler(oauth2LogoutHandler)
            .logoutSuccessUrl("/");
//            .and()
//            .exceptionHandling().authenticationEntryPoint(unauthorizedHandler);
        http.oauth2ResourceServer().jwt();
        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        return http.build();
    }

    private static AuthenticationFailureHandler getAuthenticationFailureHandler(ObjectMapper mapper) {
        return (request, response, exception) -> {
            AuthRespDTO authResponse = AuthRespDTO.builder().message(exception.getMessage()).build();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write(mapper.writeValueAsString(authResponse));
        };
    }

    private AuthenticationSuccessHandler getSuccessHandler(TokenProvider tokenUtils, ModelMapper modelMapper) {
        return new JwtAuthSuccessHandler(tokenUtils, modelMapper);
    }
}
