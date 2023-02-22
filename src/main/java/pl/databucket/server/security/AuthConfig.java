package pl.databucket.server.security;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuthConfig {

    @Resource(name = "userService")
    UserDetailsService userDetailsService;
    AuthenticationManagerBuilder auth;
    BCryptPasswordEncoder encoder;

    @PostConstruct
    public void globalUserDetails() throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(encoder);
    }

}
