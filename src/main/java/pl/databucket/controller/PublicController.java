package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.AuthDtoRequest;
import pl.databucket.dto.AuthDtoResponse;
import pl.databucket.dto.AuthProjectDto;
import pl.databucket.dto.DataGetDto;
import pl.databucket.entity.Project;
import pl.databucket.entity.User;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.repository.UserRepository;
import pl.databucket.response.MessageResponse;
import pl.databucket.security.TokenProvider;
import pl.databucket.service.data.DataService;
import pl.databucket.service.data.QueryRule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataService dataService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TokenProvider jwtTokenUtil;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(PublicController.class);

    @PostMapping(value = "/signin")
    public ResponseEntity<?> signIn(@RequestBody AuthDtoRequest userDto) {
        User user = userRepository.findByUsername(userDto.getUsername());
        if (user == null)
            return exceptionFormatter.customException(new UsernameNotFoundException("Bad credentials"), HttpStatus.UNAUTHORIZED);

        try {
            AuthDtoResponse authDtoResponse = new AuthDtoResponse();
            HttpStatus status = HttpStatus.OK;

            final Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDto.getUsername(), userDto.getPassword()));

            if (user.isExpired()) {
                authDtoResponse.setMessage("User access has expired!");
                return new ResponseEntity<>(authDtoResponse, HttpStatus.FORBIDDEN);
            }

            // The user has to change password. We force this action.
            if (user.isChangePassword()) {
                authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, userDto.getProjectId() != null ? userDto.getProjectId() : 0));
                authDtoResponse.setChangePassword(true);

                // We've got the projectId that user want to sign into
            } else if (userDto.getProjectId() != null) {

                    // User is assigned to given project
                if (user.getProjects().stream().anyMatch(o -> o.getId() == userDto.getProjectId())) {
                    Project project = user.getProjects().stream().filter(o -> o.getId() == userDto.getProjectId()).findFirst().get();

                    if (!project.getEnabled()) {
                        authDtoResponse.setMessage("The project is disabled!");
                        status = HttpStatus.FORBIDDEN;

                        // The project is expired
                    } else if (project.isExpired()) {
                        authDtoResponse.setMessage("The project expired!");
                        status = HttpStatus.FORBIDDEN;

                        // The project is enabled and not expired
                    } else {
                        AuthProjectDto authProjectDto = new AuthProjectDto();
                        modelMapper.map(project, authProjectDto);
                        List<String> roles = user.getRoles().stream().map(item -> modelMapper.map(item.getName(), String.class)).collect(Collectors.toList());

                        authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, userDto.getProjectId()));
                        authDtoResponse.setProject(authProjectDto);
                        authDtoResponse.setRoles(roles);
                    }

                    // The user is not assigned to requested project
                } else {
                    authDtoResponse.setMessage("The user is not assigned to given project!");
                    status = HttpStatus.FORBIDDEN;
                }

                // We haven't got the projectId but the user is assigned to one project.
            } else if (userDto.getProjectId() == null && user.getProjects().size() == 1) {
                Project project = user.getProjects().iterator().next();

                // The project is disabled
                if (!project.getEnabled()) {
                    authDtoResponse.setMessage("The project is disabled!");
                    status = HttpStatus.FORBIDDEN;

                    // The project is expired
                } else if (project.isExpired()) {
                    authDtoResponse.setMessage("The project expired!");
                    status = HttpStatus.FORBIDDEN;

                    // The project is enabled and not expired
                } else {
                    List<AuthProjectDto> projects = user.getProjects().stream().map(item -> modelMapper.map(item, AuthProjectDto.class)).collect(Collectors.toList());
                    List<String> roles = user.getRoles().stream().map(item -> modelMapper.map(item.getName(), String.class)).collect(Collectors.toList());
                    AuthProjectDto authProjectDto = new AuthProjectDto();
                    modelMapper.map(project, authProjectDto);

                    authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, projects.get(0).getId()));
                    authDtoResponse.setProject(authProjectDto);
                    authDtoResponse.setRoles(roles);
                }

                // We haven't got the projectId, and the user is assigned to more then one project. We return all projects to which the user is assigned.
            } else if (userDto.getProjectId() == null && user.getProjects().size() > 1) {
                List<AuthProjectDto> projects = user.getProjects().stream().map(item -> modelMapper.map(item, AuthProjectDto.class)).collect(Collectors.toList());
                List<String> roles = user.getRoles().stream().map(item -> modelMapper.map(item.getName(), String.class)).collect(Collectors.toList());

                authDtoResponse.setProjects(projects);
                authDtoResponse.setRoles(roles);

                if (user.isSuperUser())
                    authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, 0));

                // We haven't got he projectId, and the user is not assigned to any project, but the user is the SUPER user.
            } else if (userDto.getProjectId() == null && user.getProjects().size() == 0 && user.isSuperUser()) {
                List<String> roles = user.getRoles().stream().map(item -> modelMapper.map(item.getName(), String.class)).collect(Collectors.toList());

                authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, 0));
                authDtoResponse.setProjects(new ArrayList<>());
                authDtoResponse.setRoles(roles);

                // We haven't got the projectId, and the user is not assigned to any project and this user is not SUPER user.
            } else {
                authDtoResponse.setMessage("The user is not assigned to any project!");
                status = HttpStatus.FORBIDDEN;
            }

            return new ResponseEntity<>(authDtoResponse, status);

        } catch (AuthenticationException e) {
            return exceptionFormatter.customException(userDto.getUsername(), e, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PostMapping(value = "/query")
    public ResponseEntity<?> getQuery(@RequestBody(required = false) DataGetDto dataGetDto) {
        try {
            return new ResponseEntity<>(new MessageResponse(dataService.getQuery(new QueryRule(dataGetDto))), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PostMapping(value = "/query-rules")
    public ResponseEntity<?> getQueryRules(@RequestBody(required = false) DataGetDto dataGetDto) {
        try {
            return new ResponseEntity<>(new QueryRule(dataGetDto), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
