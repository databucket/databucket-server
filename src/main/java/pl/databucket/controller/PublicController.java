package pl.databucket.controller;

import org.springframework.http.HttpStatus;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.entity.User;
import pl.databucket.security.TokenProvider;
import pl.databucket.dto.UserDto;
import pl.databucket.dto.AuthTokenDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.databucket.service.UserService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenProvider jwtTokenUtil;

    @Autowired
    private UserService userService;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(PublicController.class);


    @PostMapping(value = "/signin")
    public ResponseEntity<?> signIn(@RequestBody UserDto userBean) {
        try {
            final Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userBean.getName(), userBean.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            final String token = jwtTokenUtil.generateToken(authentication);

            User user = userService.findByName(userBean.getName());

            return ResponseEntity.ok(new AuthTokenDto(token, user.isChangePassword()));
        } catch (AuthenticationException e) {
            return exceptionFormatter.customException(e, HttpStatus.UNAUTHORIZED);
        }
    }
}
