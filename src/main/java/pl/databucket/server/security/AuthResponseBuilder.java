package pl.databucket.server.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import pl.databucket.server.dto.AuthProjectDTO;
import pl.databucket.server.dto.AuthRespDTO;
import pl.databucket.server.dto.AuthRespDTO.AuthRespDTOBuilder;
import pl.databucket.server.entity.Project;
import pl.databucket.server.exception.AuthForbiddenException;

@Component
@RequiredArgsConstructor
public class AuthResponseBuilder {

    private final TokenProvider tokenUtils;
    private final ModelMapper modelMapper;

    public AuthRespDTO buildAuthResponse(UsernamePasswordAuthenticationToken authentication, String projectid) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        AuthRespDTOBuilder responseBuilder = AuthRespDTO.builder().username(user.getUsername());

        if (user.isExpired()) {
            responseBuilder.message("User access has expired!");
            throw new AuthForbiddenException(responseBuilder.build());
        }
        int parsedProjectId =
            !ObjectUtils.isEmpty(projectid) && projectid.matches("\\d+") ? Integer.parseInt(projectid) : 0;

        // The user has to change password. We force this action.
        if (user.isChangePassword()) {
            responseBuilder
                .token(tokenUtils.generateToken(authentication, parsedProjectId))
                .changePassword(true);

            // We've got the projectId that user want to sign into
        } else if (parsedProjectId != 0) {
            // User is assigned to given project
            Optional<Project> targetProject = user.getProjects().stream().filter(o -> o.getId() == parsedProjectId)
                .findFirst();
            if (targetProject.isPresent()) {
                if (targetProject.filter(Project::getEnabled).isEmpty()) {
                    responseBuilder.message("The project is disabled!");
                    throw new AuthForbiddenException(responseBuilder.build());
                    // The project is expired
                } else if (targetProject.filter(Project::isExpired).isPresent()) {
                    responseBuilder.message("The project expired!");
                    throw new AuthForbiddenException(responseBuilder.build());

                    // The project is enabled and not expired
                } else {
                    AuthProjectDTO authProjectDto = new AuthProjectDTO();
                    modelMapper.map(targetProject.get(), authProjectDto);
                    List<String> roles = user.getAuthorities().stream()
                        .map(item -> modelMapper.map(item.getAuthority(), String.class))
                        .toList();

                    responseBuilder.token(tokenUtils.generateToken(authentication, parsedProjectId))
                        .project(authProjectDto)
                        .roles(roles);
                }

                // The user is not assigned to requested project
            } else {
                List<String> roles = user.getAuthorities().stream()
                    .map(item -> modelMapper.map(item.getAuthority(), String.class)).toList();
                responseBuilder
                    .message("The user is not assigned to given project!")
                    .token(tokenUtils.generateToken(authentication, 0))
                    .projects(List.of())
                    .roles(roles);
            }

            // We haven't got the projectId but the user is assigned to one project.
        } else if (Optional.ofNullable(user.getProjects()).map(Set::size).orElse(0) == 1) {
            Project project = user.getProjects().iterator().next();

            // The project is disabled
            if (!project.getEnabled()) {
                responseBuilder.message("The project is disabled!");
                throw new AuthForbiddenException(responseBuilder.build());

                // The project is expired
            } else if (project.isExpired()) {
                responseBuilder.message("The project expired!");
                throw new AuthForbiddenException(responseBuilder.build());
                // The project is enabled and not expired
            } else {
                List<AuthProjectDTO> projects = user.getProjects().stream()
                    .map(item -> modelMapper.map(item, AuthProjectDTO.class)).toList();
                List<String> roles = user.getAuthorities().stream()
                    .map(item -> modelMapper.map(item.getAuthority(), String.class)).toList();
                AuthProjectDTO authProjectDto = new AuthProjectDTO();
                modelMapper.map(project, authProjectDto);

                responseBuilder.token(tokenUtils.generateToken(authentication, projects.get(0).getId()))
                    .project(authProjectDto)
                    .roles(roles);
            }

            // We haven't got the projectId, and the user is assigned to more then one project. We return all projects to which the user is assigned.
        } else if (Optional.ofNullable(user.getProjects()).map(Set::size).orElse(0) > 1) {
            List<AuthProjectDTO> projects = user.getProjects().stream()
                .map(item -> modelMapper.map(item, AuthProjectDTO.class))
                .sorted(Comparator.comparing(AuthProjectDTO::getId))
                .toList();
            List<String> roles = user.getAuthorities().stream()
                .map(item -> modelMapper.map(item.getAuthority(), String.class))
                .toList();

            responseBuilder.projects(projects)
                .roles(roles)
                .token(tokenUtils.generateToken(authentication, 0));

//            if (user.isSuperUser()) {
//                responseBuilder.token(tokenUtils.generateToken(authentication, 0));
//            }

            // We haven't got he projectId, and the user is not assigned to any project, but the user is the SUPER user.
        } else if (user.isSuperUser()) {
            List<String> roles = user.getAuthorities().stream()
                .map(item -> modelMapper.map(item.getAuthority(), String.class))
                .toList();

            responseBuilder.token(tokenUtils.generateToken(authentication, 0))
                .projects(new ArrayList<>())
                .roles(roles);

            // We haven't got the projectId, and the user is not assigned to any project and this user is not SUPER user.
        } else {
            List<String> roles = user.getAuthorities().stream()
                .map(item -> modelMapper.map(item.getAuthority(), String.class)).toList();
            responseBuilder
                .message("The user is not assigned to any project!")
                .token(tokenUtils.generateToken(authentication, 0))
                .project(new AuthProjectDTO())
                .roles(roles);
        }
        return responseBuilder.build();
    }
}
