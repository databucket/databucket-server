package pl.databucket.server.security;

import javax.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    public AuthenticationManagerResolver<HttpServletRequest> resolver() {
        return request -> customersAuthenticationManager();
    }

    @Bean
    public AuthenticationManager customersAuthenticationManager() {
        return authentication -> new UsernamePasswordAuthenticationToken(
            authentication.getPrincipal(),
            authentication.getCredentials());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationEntryPoint unauthorizedHandler)
        throws Exception {
        http.sessionManagement().invalidSessionUrl("/");
        http.cors().and().csrf().disable()
            .authorizeHttpRequests()
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
                "/index*", "/static/**", "/*.js", "/*.json", "/*.ico",
                "/api/public/auth-options",
                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                "/actuator/**")
            .permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2Login()
            .loginPage("/auth")
            .and()
            .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

//        http.addFilterBefore(authenticationTokenFilterBean(), UsernamePasswordAuthenticationFilter.class);
//        http.addFilterBefore(authenticationFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter authenticationTokenFilterBean() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    private AuthenticationFilter authenticationFilter() {
        AuthenticationFilter filter = new AuthenticationFilter(
            resolver(), authenticationConverter());
        filter.setSuccessHandler((request, response, auth) -> {
        });
        return filter;
    }

    public AuthenticationConverter authenticationConverter() {
        return new BasicAuthenticationConverter();
    }

}
