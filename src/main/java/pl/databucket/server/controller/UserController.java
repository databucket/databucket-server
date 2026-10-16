package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.*;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.NoAccessToProjectException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.security.CustomUserDetails;
import pl.databucket.server.security.TokenProvider;
import pl.databucket.server.service.UserService;

import jakarta.validation.Valid;
import java.util.List;

@Hidden
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/users")
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenProvider jwtTokenUtil;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(UserController.class);


    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @GetMapping
    public ResponseEntity<?> getUsers() {
        try {
            List<UserDtoResponse> usersDto = userService.getUsers(((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getProjectId());
            return new ResponseEntity<>(usersDto, HttpStatus.OK);
        } catch (IllegalArgumentException | ItemNotFoundException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<?> modifyUser(@Valid @RequestBody UserDtoRequest userDtoRequest) {
        try {
            UserDtoResponse userDtoResponse = userService.modifyUser(userDtoRequest);
            return new ResponseEntity<>(userDtoResponse, HttpStatus.OK);
        } catch (SomeItemsNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }


    @PostMapping(value = "/password/change")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDtoRequest changePasswordDtoRequest) {
        try {
            userService.changePassword(changePasswordDtoRequest);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    /**
     * Can be used when changing project
     */
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @PostMapping(value = "/change-project")
    public ResponseEntity<?> changeProject(@Valid @RequestBody AuthReqDTO userDto) {
        try {
            userService.checkProjectAccess(SecurityContextHolder.getContext().getAuthentication().getName(), userDto.getProjectId());
            AuthRespDTO authDtoResponse = new AuthRespDTO();
            authDtoResponse.setToken(jwtTokenUtil.generateToken(SecurityContextHolder.getContext().getAuthentication(), userDto.getProjectId()));
            AuditLog.log("auth.change-project", "project={}", userDto.getProjectId());
            return new ResponseEntity<>(authDtoResponse, HttpStatus.OK);
        } catch (NoAccessToProjectException e) {
            AuditLog.log("auth.change-project-denied", "project={} reason={}", userDto.getProjectId(), e.getMessage());
            return exceptionFormatter.customException(e, HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

}
