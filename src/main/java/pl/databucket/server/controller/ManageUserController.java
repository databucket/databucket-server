package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.AuthReqDTO;
import pl.databucket.server.dto.ManageUserDtoRequest;
import pl.databucket.server.dto.ManageUserDtoResponse;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.service.ManageUserService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@PreAuthorize("hasRole('SUPER')")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/manage/users")
@RestController
public class ManageUserController {

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private ModelMapper modelMapper;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(ManageUserController.class);

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody ManageUserDtoRequest manageUserDtoRequest) {
        try {
            User user = manageUserService.createUser(manageUserDtoRequest);
            ManageUserDtoResponse manageUserDtoResponse = new ManageUserDtoResponse();
            modelMapper.map(user, manageUserDtoResponse);
            return new ResponseEntity<>(manageUserDtoResponse, HttpStatus.CREATED);
        } catch (ItemAlreadyExistsException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (SomeItemsNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getUsers() {
        try {
            List<User> users = manageUserService.getUsers();
            List<ManageUserDtoResponse> usersDto = users.stream().map(item -> modelMapper.map(item, ManageUserDtoResponse.class)).collect(Collectors.toList());
            return new ResponseEntity<>(usersDto, HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyUser(@Valid @RequestBody ManageUserDtoRequest userDtoRequest) {
        try {
            User user = manageUserService.modifyUser(userDtoRequest);
            ManageUserDtoResponse manageUserDtoResponse = new ManageUserDtoResponse();
            modelMapper.map(user, manageUserDtoResponse);
            return new ResponseEntity<>(manageUserDtoResponse, HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PostMapping(value = "/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody AuthReqDTO authDtoRequest) {
        try {
            manageUserService.resetPassword(authDtoRequest);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }
}
