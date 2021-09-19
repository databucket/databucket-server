package pl.databucket.server.controller;

import io.swagger.annotations.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.AuthReqDTO;
import pl.databucket.server.dto.AuthRespDTO;
import pl.databucket.server.dto.AuthProjectDTO;
import pl.databucket.server.dto.DataGetDTO;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.repository.UserRepository;
import pl.databucket.server.response.MessageResponse;
import pl.databucket.server.security.TokenProvider;
import pl.databucket.server.service.data.DataService;
import pl.databucket.server.service.data.QueryRule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Api(tags="PUBLIC")
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


    @ApiOperation(value = "Authenticate", notes = "Gets the token nedded to work on data", response = AuthRespDTO.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = AuthRespDTO.class)
    })
    @PostMapping(value = "/signin", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signIn(@ApiParam(value="Payload") @RequestBody AuthReqDTO authReqDTO) {
        User user = userRepository.findByUsername(authReqDTO.getUsername());
        if (user == null)
            return exceptionFormatter.customException(new UsernameNotFoundException("Bad credentials"), HttpStatus.UNAUTHORIZED);

        try {
            AuthRespDTO authDtoResponse = new AuthRespDTO();
            HttpStatus status = HttpStatus.OK;

            final Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authReqDTO.getUsername(), authReqDTO.getPassword()));

            if (user.isExpired()) {
                authDtoResponse.setMessage("User access has expired!");
                return new ResponseEntity<>(authDtoResponse, HttpStatus.FORBIDDEN);
            }

            // The user has to change password. We force this action.
            if (user.isChangePassword()) {
                authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, authReqDTO.getProjectId() != null ? authReqDTO.getProjectId() : 0));
                authDtoResponse.setChangePassword(true);

                // We've got the projectId that user want to sign into
            } else if (authReqDTO.getProjectId() != null) {

                // User is assigned to given project
                if (user.getProjects().stream().anyMatch(o -> o.getId() == authReqDTO.getProjectId())) {
                    Project project = user.getProjects().stream().filter(o -> o.getId() == authReqDTO.getProjectId()).findFirst().get();

                    if (!project.getEnabled()) {
                        authDtoResponse.setMessage("The project is disabled!");
                        status = HttpStatus.FORBIDDEN;

                        // The project is expired
                    } else if (project.isExpired()) {
                        authDtoResponse.setMessage("The project expired!");
                        status = HttpStatus.FORBIDDEN;

                        // The project is enabled and not expired
                    } else {
                        AuthProjectDTO authProjectDto = new AuthProjectDTO();
                        modelMapper.map(project, authProjectDto);
                        List<String> roles = user.getRoles().stream().map(item -> modelMapper.map(item.getName(), String.class)).collect(Collectors.toList());

                        authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, authReqDTO.getProjectId()));
                        authDtoResponse.setProject(authProjectDto);
                        authDtoResponse.setRoles(roles);
                    }

                    // The user is not assigned to requested project
                } else {
                    authDtoResponse.setMessage("The user is not assigned to given project!");
                    status = HttpStatus.FORBIDDEN;
                }

                // We haven't got the projectId but the user is assigned to one project.
            } else if (authReqDTO.getProjectId() == null && user.getProjects().size() == 1) {
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
                    List<AuthProjectDTO> projects = user.getProjects().stream().map(item -> modelMapper.map(item, AuthProjectDTO.class)).collect(Collectors.toList());
                    List<String> roles = user.getRoles().stream().map(item -> modelMapper.map(item.getName(), String.class)).collect(Collectors.toList());
                    AuthProjectDTO authProjectDto = new AuthProjectDTO();
                    modelMapper.map(project, authProjectDto);

                    authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, projects.get(0).getId()));
                    authDtoResponse.setProject(authProjectDto);
                    authDtoResponse.setRoles(roles);
                }

                // We haven't got the projectId, and the user is assigned to more then one project. We return all projects to which the user is assigned.
            } else if (authReqDTO.getProjectId() == null && user.getProjects().size() > 1) {
                List<AuthProjectDTO> projects = user.getProjects().stream().map(item -> modelMapper.map(item, AuthProjectDTO.class)).collect(Collectors.toList());
                List<String> roles = user.getRoles().stream().map(item -> modelMapper.map(item.getName(), String.class)).collect(Collectors.toList());

                authDtoResponse.setProjects(projects);
                authDtoResponse.setRoles(roles);

                if (user.isSuperUser())
                    authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, 0));

                // We haven't got he projectId, and the user is not assigned to any project, but the user is the SUPER user.
            } else if (authReqDTO.getProjectId() == null && user.getProjects().size() == 0 && user.isSuperUser()) {
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
            return exceptionFormatter.customException(authReqDTO.getUsername(), e, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @ApiOperation(value = "getQuery", hidden = true)
    @PostMapping(value = "/query")
    public ResponseEntity<?> getQuery(@RequestBody(required = false) DataGetDTO dataGetDto) {
        try {
            return new ResponseEntity<>(new MessageResponse(dataService.getQuery(new QueryRule("fakeUserName", dataGetDto))), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @ApiOperation(value = "getQueryRules", hidden = true)
    @PostMapping(value = "/query-rules")
    public ResponseEntity<?> getQueryRules(@RequestBody(required = false) DataGetDTO dataGetDto) {
        try {
            return new ResponseEntity<>(new QueryRule("fakeUserName", dataGetDto), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
