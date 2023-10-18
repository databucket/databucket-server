package pl.databucket.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
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
import pl.databucket.server.repository.RoleRepository;
import pl.databucket.server.service.ManageUserService;
import pl.databucket.server.service.UserService;

@Log4j2
@Configuration
public class OAuth2SecurityConfig {

    @Bean
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http,
        OAuth2LogoutHandler oauth2LogoutHandler,
        AuthResponseBuilder authResponseBuilder,
        ObjectMapper mapper,
        AuthenticationSuccessHandler oAuth2SuccessHandler) throws Exception {
        AuthenticationSuccessHandler successHandler = getFormSuccessHandler(authResponseBuilder);
        AuthenticationFailureHandler failureHandler = getAuthenticationFailureHandler(mapper);
        http.cors().and().csrf().disable()
            .authorizeRequests()
            .antMatchers(HttpMethod.GET,
                "/", "/login**", "/sign-up", "/forgot-password", "/change-password",
                "/index*", "/**/static/**", "/*.js", "/*.json", "/*.ico",
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
            .permitAll()
            .and()
            .oauth2Login()
            .successHandler(oAuth2SuccessHandler)
            .failureHandler(failureHandler)
            .and()
            .logout()
            .addLogoutHandler(oauth2LogoutHandler)
            .deleteCookies("JSESSIONID")
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
            log.error("Auth error", exception);
            AuthRespDTO authResponse = AuthRespDTO.builder().message(exception.getMessage()).build();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write(mapper.writeValueAsString(authResponse));
        };
    }

    private AuthenticationSuccessHandler getFormSuccessHandler(AuthResponseBuilder authResponseBuilder) {
        return new FormAuthSuccessHandler(authResponseBuilder);
    }

    @Bean
    public AuthenticationSuccessHandler oAuth2SuccessHandler(
        AuthResponseBuilder authResponseBuilder, ManageUserService manageUserService, RoleRepository roleRepository,
        UserService userService) {
        return new OAuth2AuthSuccessHandler(authResponseBuilder, manageUserService, roleRepository, userService);
    }
}
