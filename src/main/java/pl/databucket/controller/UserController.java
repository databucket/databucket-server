package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.*;
import pl.databucket.entity.*;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.SomeItemsNotFoundException;
import pl.databucket.security.CustomUserDetails;
import pl.databucket.security.TokenProvider;
import pl.databucket.service.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/users")
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TokenProvider jwtTokenUtil;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(UserController.class);


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<?> getUsers() {
        try {
            List<User> users = userService.getUsers(((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getProjectId());
            List<UserDtoResponse> usersDto = users.stream().map(item -> modelMapper.map(item, UserDtoResponse.class)).collect(Collectors.toList());
            return new ResponseEntity<>(usersDto, HttpStatus.OK);
        } catch (IllegalArgumentException | ItemNotFoundException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<?> modifyUser(@Valid @RequestBody UserDtoRequest userDtoRequest) {
        try {
            User user = userService.modifyUser(userDtoRequest);
            UserDtoResponse userDtoResponse = new UserDtoResponse();
            modelMapper.map(user, userDtoResponse);
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
            AuthRespDTO authDtoResponse = new AuthRespDTO();
            authDtoResponse.setToken(jwtTokenUtil.generateToken(SecurityContextHolder.getContext().getAuthentication(), userDto.getProjectId()));
            return new ResponseEntity<>(authDtoResponse, HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

}
