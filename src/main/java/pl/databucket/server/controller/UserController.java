package pl.databucket.server.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.dto.AuthReqDTO;
import pl.databucket.server.dto.AuthRespDTO;
import pl.databucket.server.dto.ChangePasswordDtoRequest;
import pl.databucket.server.dto.UserDtoRequest;
import pl.databucket.server.dto.UserDtoResponse;
import pl.databucket.server.entity.Team;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.security.CustomUserDetails;
import pl.databucket.server.security.TokenProvider;
import pl.databucket.server.service.TeamService;
import pl.databucket.server.service.UserService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/users")
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TokenProvider jwtTokenUtil;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(UserController.class);


    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @GetMapping
    public ResponseEntity<?> getUsers() {
        try {
            List<User> users = userService.getUsers(
                ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication()
                    .getPrincipal()).getProjectId());
            Set<Short> projectTeams = teamService.getTeams().stream().map(Team::getId).collect(Collectors.toSet());
            List<UserDtoResponse> usersDto = users.stream().map(item -> modelMapper.map(item, UserDtoResponse.class))
                .collect(Collectors.toList());

            // filter teams from current project
            for (UserDtoResponse userDto : usersDto) {
                if (userDto.getTeamsIds() != null) {
                    userDto.getTeamsIds().retainAll(projectTeams);
                }
            }

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
            AuthRespDTO authDtoResponse = AuthRespDTO.builder()
                .token(jwtTokenUtil.generateToken(SecurityContextHolder.getContext().getAuthentication(),
                    userDto.getProjectId()))
                .build();
            return new ResponseEntity<>(authDtoResponse, HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

}
