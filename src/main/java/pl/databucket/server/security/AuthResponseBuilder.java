package pl.databucket.server.security;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pl.databucket.server.dto.AuthProjectDTO;
import pl.databucket.server.dto.AuthRespDTO;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.User;
import pl.databucket.server.logging.AuditLog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the sign-in response (token, project or list of projects, roles) for an already authenticated user.
 * Shared by the password sign-in and the SSO sign-in.
 */
@Component
public class AuthResponseBuilder {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TokenProvider jwtTokenUtil;

    /**
     * @param user           the user with projects and roles loaded
     * @param authentication the authentication whose name and authorities go into the token
     * @param projectId      the project to sign into, or null to pick the only one / list them
     * @param passwordLogin  whether the user signed in with a password; only then a pending password change is enforced
     * @param auditEvent     audit event logged on success; "-refused" is appended when access is refused
     */
    public ResponseEntity<AuthRespDTO> build(User user, Authentication authentication, Integer projectId, boolean passwordLogin, String auditEvent) {
        AuthRespDTO authDtoResponse = new AuthRespDTO();
        authDtoResponse.setUsername(user.getUsername());
        HttpStatus status = HttpStatus.OK;

        if (user.isExpired()) {
            authDtoResponse.setMessage("User access has expired!");
            AuditLog.log(auditEvent + "-refused", "user={} reason={}", user.getUsername(), authDtoResponse.getMessage());
            return new ResponseEntity<>(authDtoResponse, HttpStatus.FORBIDDEN);
        }

        // The user has to change password. We force this action.
        if (passwordLogin && user.isChangePassword()) {
            authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, projectId != null ? projectId : 0));
            authDtoResponse.setChangePassword(true);

            // We've got the projectId that user want to sign into
        } else if (projectId != null) {

            // User is assigned to given project
            if (user.getProjects().stream().anyMatch(o -> o.getId() == projectId)) {
                Project project = user.getProjects().stream().filter(o -> o.getId() == projectId).findFirst().get();

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

                    authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, projectId));
                    authDtoResponse.setProject(authProjectDto);
                    authDtoResponse.setRoles(roles);
                }

                // The user is not assigned to requested project
            } else {
                authDtoResponse.setMessage("The user is not assigned to given project!");
                status = HttpStatus.FORBIDDEN;
            }

            // We haven't got the projectId but the user is assigned to one project.
        } else if (user.getProjects().size() == 1) {
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
        } else if (user.getProjects().size() > 1) {
            List<AuthProjectDTO> projects = user.getProjects().stream()
                    .map(item -> modelMapper.map(item, AuthProjectDTO.class))
                    .sorted(Comparator.comparing(AuthProjectDTO::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .collect(Collectors.toList());
            List<String> roles = user.getRoles().stream().map(item -> modelMapper.map(item.getName(), String.class)).collect(Collectors.toList());

            authDtoResponse.setProjects(projects);
            authDtoResponse.setRoles(roles);

            if (user.isSuperUser())
                authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, 0));

            // We haven't got he projectId, and the user is not assigned to any project, but the user is the SUPER user.
        } else if (user.getProjects().isEmpty() && user.isSuperUser()) {
            List<String> roles = user.getRoles().stream().map(item -> modelMapper.map(item.getName(), String.class)).collect(Collectors.toList());

            authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, 0));
            authDtoResponse.setProjects(new ArrayList<>());
            authDtoResponse.setRoles(roles);

            // We haven't got the projectId, and the user is not assigned to any project and this user is not SUPER user.
        } else {
            authDtoResponse.setMessage("The user is not assigned to any project!");
            status = HttpStatus.FORBIDDEN;
        }

        if (status == HttpStatus.OK)
            AuditLog.log(auditEvent, "user={} project={}", user.getUsername(),
                    authDtoResponse.getProject() != null ? authDtoResponse.getProject().getId() : null);
        else
            AuditLog.log(auditEvent + "-refused", "user={} reason={}", user.getUsername(), authDtoResponse.getMessage());
        return new ResponseEntity<>(authDtoResponse, status);
    }
}
