package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.*;
import pl.databucket.entity.Role;
import pl.databucket.entity.User;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.ItemAlreadyExistsException;
import pl.databucket.exception.SomeItemsNotFoundException;
import pl.databucket.response.UserPageResponse;
import pl.databucket.service.UserService;
import pl.databucket.specification.UserSpecification;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

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

    @PreAuthorize("hasAnyRole('SUPER')")
    @PostMapping(value = "/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody AuthDtoRequest userDto) {
        try {
            userService.resetPassword(userDto);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN', 'MEMBER')")
    @PostMapping(value = "/password/change")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDtoRequest changePasswordDtoRequest) {
        try {
            userService.changePassword(changePasswordDtoRequest);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @GetMapping(value = "/roles")
    public ResponseEntity<?> getRoles() {
        try {
            List<Role> roles = userService.getRoles();
            List<RoleDto> rolesDto = roles.stream().map(item -> modelMapper.map(item, RoleDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(rolesDto, HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

}
