package pl.databucket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.AuthDto;
import pl.databucket.dto.UserDtoRequest;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.security.CustomUserDetails;
import pl.databucket.security.TokenProvider;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenProvider jwtTokenUtil;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(PublicController.class);

    @PostMapping(value = "/signin")
    public ResponseEntity<?> signIn(@RequestBody UserDtoRequest userDto) {
        try {
            final Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDto.getName(), userDto.getPassword()));

            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

            SecurityContextHolder.getContext().setAuthentication(authentication);

            AuthDto authDto = new AuthDto();
            if (customUserDetails.getProjectId() != null) {
                final String token = jwtTokenUtil.generateToken(authentication, customUserDetails.getProjectId());
                authDto.setToken(token);
                authDto.setChangePassword(customUserDetails.isChangePassword());
            } else if (userDto.getProjectId() != null) {
                if (customUserDetails.getProjects().stream().anyMatch(o -> o.getId() == userDto.getProjectId())) {
                    final String token = jwtTokenUtil.generateToken(authentication, userDto.getProjectId());
                    authDto.setToken(token);
                    authDto.setChangePassword(customUserDetails.isChangePassword());
                } else
                    authDto.setMessage("The user is not assigned to given project!");
            } else if (customUserDetails.getProjects() != null && customUserDetails.getProjects().size() > 0) {
                authDto.setProjects(customUserDetails.getProjects());
            } else
                authDto.setMessage("The user is not assigned to any project!");

            return ResponseEntity.ok(authDto);

        } catch (AuthenticationException e) {
            return exceptionFormatter.customException(e, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return exceptionFormatter.customException(e, HttpStatus.FORBIDDEN);
        }
    }
}
