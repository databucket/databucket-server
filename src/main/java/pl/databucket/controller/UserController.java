package pl.databucket.controller;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pl.databucket.exception.CustomExceptionFormatter;
import pl.databucket.model.beans.RoleBean;
import pl.databucket.model.beans.UserBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.model.entity.User;
import pl.databucket.service.UserService;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/user")
@RestController
public class UserController {

    @Autowired
    private UserService userService;
    private final CustomExceptionFormatter customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(UserController.class));

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN', 'MEMBER')")
    @GetMapping
    public ResponseEntity<?> getUsers(){
        try {
            List<User> users = userService.findAll();
            return ResponseEntity.ok(users);
        } catch (IllegalArgumentException e1) {
            return customExceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping(value="/signup")
    public ResponseEntity<?> signUp(@RequestBody UserBean userBean){
        try {
            User user = userService.save(userBean);
            return new ResponseEntity<>(user, HttpStatus.CREATED);
        } catch (IllegalArgumentException e1) {
            return customExceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping(value="/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody UserBean userBean){
        try {
            User user = userService.resetPassword(userBean);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e1) {
            return customExceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN', 'MEMBER')")
    @PostMapping(value="/password/change")
    public ResponseEntity<?> changePassword(@RequestBody UserBean userBean){
        try {
            User user = userService.changePassword(userBean);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e1) {
            return customExceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping(value="/role/add")
    public ResponseEntity<?> addRole(@RequestBody RoleBean roleBean) {
        try {
            User user = userService.addRole(roleBean);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e1) {
            return customExceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping(value="/role/remove")
    public ResponseEntity<?> removeRole(@RequestBody RoleBean roleBean){
        try {
            User user = userService.removeRole(roleBean);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e1) {
            return customExceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

}
