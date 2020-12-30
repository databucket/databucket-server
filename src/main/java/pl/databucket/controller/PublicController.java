package pl.databucket.controller;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import pl.databucket.exception.CustomExceptionFormatter;
import pl.databucket.model.entity.User;
import pl.databucket.security.TokenProvider;
import pl.databucket.model.beans.UserBean;
import pl.databucket.model.beans.AuthTokenBean;
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

    private final CustomExceptionFormatter customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(PublicController.class));


    @PostMapping(value = "/signin")
    public ResponseEntity<?> signIn(@RequestBody UserBean userBean) throws AuthenticationException {
        try {
            final Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userBean.getName(), userBean.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            final String token = jwtTokenUtil.generateToken(authentication);

            User user = userService.findByName(userBean.getName());

            return ResponseEntity.ok(new AuthTokenBean(token, user.isChangePassword()));
        } catch (AuthenticationException e) {
            return customExceptionFormatter.customException(e, HttpStatus.UNAUTHORIZED);
        }
    }

}
