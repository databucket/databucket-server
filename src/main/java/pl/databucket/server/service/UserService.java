package pl.databucket.server.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.ChangePasswordDtoRequest;
import pl.databucket.server.dto.UserDtoRequest;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.Team;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.repository.ProjectRepository;
import pl.databucket.server.repository.TeamRepository;
import pl.databucket.server.repository.UserRepository;
import pl.databucket.server.security.CustomUserDetails;


@Service(value = "userService")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UserService implements UserDetailsService {

    UserRepository userRepository;
    ProjectRepository projectRepository;
    TeamRepository teamRepository;
    BCryptPasswordEncoder bcryptEncoder;

    // This method is used every time when authorized user want to do something.
    // This method must be as light as possible, so most of the logic is moved into public controller when the user is trying to login
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOp = userRepository.findByUsername(username);

        return userOp.map(user -> CustomUserDetails.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(getAuthority(user))
                .enabled(user.getEnabled())
                .expired(user.isExpired())
                .superUser(user.isSuperUser())
                .changePassword(user.isChangePassword())
                .projects(user.getProjects())
                .build())
            .orElse(null);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User getCurrentUser() {
        return getUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private Set<SimpleGrantedAuthority> getAuthority(User user) {
        return user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .collect(Collectors.toSet());
    }

    public List<User> getUsers(int projectId) throws ItemNotFoundException {
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isPresent()) {
            return userRepository.findUsersByProjectsContainsOrderById(project.get());
        } else {
            throw new ItemNotFoundException(Project.class, projectId);
        }
    }


    public User modifyUser(UserDtoRequest userDtoRequest) throws SomeItemsNotFoundException {
        Optional<User> userOp = userRepository.findByUsername(userDtoRequest.getUsername());

        return Optional.ofNullable(userDtoRequest.getTeamsIds())
            .flatMap(teamIds -> {
                List<Team> teams = teamRepository.findAllByDeletedAndIdIn(false, teamIds);
                return userOp.map(user -> {
                    user.setTeams(new HashSet<>(teams));
                    return userRepository.save(user);
                });
            }).orElse(null);
    }

    public void changePassword(ChangePasswordDtoRequest changePasswordDtoRequest) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        // can not change password of another user except ROBOT
        if (!name.equals(changePasswordDtoRequest.getUsername())) {
            Optional<User> userOp = userRepository.findByUsername(changePasswordDtoRequest.getUsername());
            if (userOp.isPresent() && userOp.get().getRoles().size() == 1 && userOp.get().getRoles().stream()
                .noneMatch(role -> role.getName().equals(Constants.ROLE_ROBOT))) {
                throw new IllegalArgumentException("You cannot change the password of this user!");
            }
        }

        userRepository.findByUsername(changePasswordDtoRequest.getUsername())
            .map(user -> {
                if (!bcryptEncoder.matches(changePasswordDtoRequest.getPassword(), user.getPassword())) {
                    throw new IllegalArgumentException("Bad credentials");
                }

                user.setPassword(bcryptEncoder.encode(changePasswordDtoRequest.getNewPassword()));
                user.setChangePassword(false);
                return userRepository.save(user);
            }).orElseThrow(() -> new IllegalArgumentException("The given user does not exist."));
    }

}
