package pl.databucket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.RoleDto;
import pl.databucket.dto.UserDto;
import pl.databucket.entity.User;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.service.UserService;
import pl.databucket.specification.UserSpecification;

import javax.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/user")
@RestController
public class UserController {

    @Autowired
    private UserService userService;
    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(UserController.class);

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN', 'MEMBER')")
    @GetMapping
    public ResponseEntity<?> getUsers(UserSpecification specification, Pageable pageable){
        try {
            return new ResponseEntity<>(userService.getUsers(specification, pageable), HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping(value="/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody UserDto userDto){
        try {
            User user = userService.save(userDto);
            return new ResponseEntity<>(user, HttpStatus.CREATED);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping(value="/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody UserDto userDto){
        try {
            User user = userService.resetPassword(userDto);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN', 'MEMBER')")
    @PostMapping(value="/password/change")
    public ResponseEntity<?> changePassword(@Valid @RequestBody UserDto userDto){
        try {
            User user = userService.changePassword(userDto);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping(value="/role/add")
    public ResponseEntity<?> addRole(@Valid @RequestBody RoleDto roleDto) {
        try {
            User user = userService.addRole(roleDto);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping(value="/role/remove")
    public ResponseEntity<?> removeRole(@Valid @RequestBody RoleDto roleDto){
        try {
            User user = userService.removeRole(roleDto);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

}