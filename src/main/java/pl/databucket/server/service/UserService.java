package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
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
import pl.databucket.server.entity.*;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.repository.*;
import pl.databucket.server.security.CustomUserDetails;
import java.util.*;


@Service(value = "userService")
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private BCryptPasswordEncoder bcryptEncoder;

    // This method is used every time when authorized user want to do something.
    // This method must be as light as possible, so most of logic is moved into public controller when the user is trying to login
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        return new CustomUserDetails(
                user.getUsername(),
                user.getPassword(),
                getAuthority(user),
                user.getEnabled(),
                user.isSuperUser());
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getCurrentUser() {
        return getUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private Set<SimpleGrantedAuthority> getAuthority(User user) {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName())));
        return authorities;
    }

    public List<User> getUsers(int projectId) throws ItemNotFoundException {
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isPresent())
            return userRepository.findUsersByProjectsContainsOrderById(project.get());
        else
            throw new ItemNotFoundException(Project.class, projectId);
    }


    public User modifyUser(UserDtoRequest userDtoRequest) throws SomeItemsNotFoundException {
        User user = userRepository.findByUsername(userDtoRequest.getUsername());

        if (userDtoRequest.getTeamsIds() != null) {
            List<Team> teams = teamRepository.findAllByDeletedAndIdIn(false, userDtoRequest.getTeamsIds());
            user.setTeams(new HashSet<>(teams));
        }

        return userRepository.save(user);
    }

    public void changePassword(ChangePasswordDtoRequest changePasswordDtoRequest) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        // can not change password of another user except ROBOT
        if (!name.equals(changePasswordDtoRequest.getUsername())) {
            User user = userRepository.findByUsername(changePasswordDtoRequest.getUsername());
            if (user.getRoles().size() == 1 && user.getRoles().stream().noneMatch(role -> role.getName().equals(Constants.ROLE_ROBOT)))
                throw new IllegalArgumentException("You cannot change the password of this user!");
        }

        User user = userRepository.findByUsername(changePasswordDtoRequest.getUsername());
        if (user != null) {
            // check the current password is correct
            if (!bcryptEncoder.matches(changePasswordDtoRequest.getPassword(), user.getPassword()))
                throw new IllegalArgumentException("Bad credentials");

            user.setPassword(bcryptEncoder.encode(changePasswordDtoRequest.getNewPassword()));
            user.setChangePassword(false);
            userRepository.save(user);
        } else
            throw new IllegalArgumentException("The given user does not exist.");
    }

}
