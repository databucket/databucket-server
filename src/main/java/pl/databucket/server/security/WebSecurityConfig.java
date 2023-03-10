package pl.databucket.server.security;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Log4j2
@Configuration
public class WebSecurityConfig {


    @Bean
    public JwtAuthenticationFilter authenticationTokenFilterBean() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain formSecurityFilterChain(HttpSecurity http,
        JwtAuthenticationEntryPoint unauthorizedHandler) throws Exception {
        http.sessionManagement().invalidSessionUrl("/");

        http.cors().and().csrf().disable()
            .authorizeRequests()
            .antMatchers(
                "/api/public/**", // public endpoint
                "/**/static/**",
                "/**/favicon.ico",
                "/login*",
                "/sign-up"
            ).permitAll()
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
            .anyRequest().authenticated()
            .and()
            .formLogin()
            .loginPage("/login-form")
            .loginProcessingUrl("/public/sign-in")
            .defaultSuccessUrl("/index.html", true)
            .failureUrl("/login-form?error=true")
            .failureHandler((request, response, exception) -> {
                log.error("Oops", exception);
            })
            .and()
            .logout()
            .logoutUrl("/logout")
            .deleteCookies("JSESSIONID")
            .logoutSuccessHandler((request, response, authentication) -> {
                log.debug(authentication);
            });

        http.addFilterBefore(authenticationTokenFilterBean(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
//            .anyRequest().authenticated()
//            .and()
//            .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
//            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//
//        http.addFilterBefore(authenticationTokenFilterBean(), UsernamePasswordAuthenticationFilter.class);
//        return http.build();
    }

}
