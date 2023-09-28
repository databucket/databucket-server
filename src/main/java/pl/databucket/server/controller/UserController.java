package pl.databucket.server.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.dto.AuthRespDTO;
import pl.databucket.server.dto.ChangePasswordDtoRequest;
import pl.databucket.server.dto.UserDtoRequest;
import pl.databucket.server.dto.UserDtoResponse;
import pl.databucket.server.entity.Team;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.security.TokenProvider;
import pl.databucket.server.service.TeamService;
import pl.databucket.server.service.UserService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/users")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TeamService teamService;
    private final ModelMapper modelMapper;
    private final TokenProvider jwtTokenUtil;
    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(UserController.class);


    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @GetMapping
    public ResponseEntity<?> getUsers(Authentication auth) {
        try {
            Jwt jwt = (Jwt) auth.getPrincipal();
            Long projectId = jwt.getClaim(TokenProvider.PROJECT_ID);
            List<User> users = userService.getUsers(projectId.intValue());
            Set<Short> projectTeams = teamService.getTeams().stream().map(Team::getId).collect(Collectors.toSet());
            List<UserDtoResponse> usersDto = users.stream().map(item -> modelMapper.map(item, UserDtoResponse.class))
                .toList();

            // filter teams from current project
            for (UserDtoResponse userDto : usersDto) {
                if (userDto.getTeamsIds() != null) {
                    userDto.getTeamsIds().retainAll(projectTeams);
                }
            }

            return ResponseEntity.ok(usersDto);
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
            return ResponseEntity.ok(userDtoResponse);
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
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    /**
     * Can be used when changing project
     */
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @PutMapping(value = "/change-project")
    public ResponseEntity<?> changeProject(
        @Valid @RequestParam int projectId, Authentication auth) {
        try {
            AuthRespDTO authDtoResponse = AuthRespDTO.builder()
                .token(jwtTokenUtil.generateToken(auth, projectId))
                .build();
            return ResponseEntity.ok(authDtoResponse);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

}
