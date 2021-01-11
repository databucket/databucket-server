package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.AuthDtoRequest;
import pl.databucket.dto.UserDtoRequest;
import pl.databucket.dto.UserDtoResponse;
import pl.databucket.entity.User;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.ItemAlreadyExistsException;
import pl.databucket.exception.SomeItemsNotFoundException;
import pl.databucket.response.UserPageResponse;
import pl.databucket.service.UserService;
import pl.databucket.specification.UserSpecification;

import javax.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/users")
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(UserController.class);

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDtoRequest userDtoRequest) {
        try {
            User user = userService.createUser(userDtoRequest);
            UserDtoResponse userDtoResponse = new UserDtoResponse();
            modelMapper.map(user, userDtoResponse);
            return new ResponseEntity<>(userDtoResponse, HttpStatus.CREATED);
        } catch (ItemAlreadyExistsException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (SomeItemsNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN', 'MEMBER')")
    @GetMapping
    public ResponseEntity<?> getUsers(UserSpecification specification, Pageable pageable) {
        try {
            return new ResponseEntity<>(new UserPageResponse(userService.getUsers(specification, pageable), modelMapper), HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
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

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping(value = "/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody AuthDtoRequest userDto) {
        try {
            User user = userService.resetPassword(userDto);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN', 'MEMBER')")
    @PostMapping(value = "/password/change")
    public ResponseEntity<?> changePassword(@Valid @RequestBody AuthDtoRequest userDto) {
        try {
            User user = userService.changePassword(userDto);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

}
