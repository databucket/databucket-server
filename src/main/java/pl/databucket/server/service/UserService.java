package pl.databucket.server.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.ChangePasswordDtoRequest;
import pl.databucket.server.dto.UserDtoRequest;
import pl.databucket.server.dto.TeamDto;
import pl.databucket.server.dto.UserDtoResponse;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.Team;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.repository.ProjectRepository;
import pl.databucket.server.repository.TeamRepository;
import pl.databucket.server.repository.UserRepository;
import pl.databucket.server.security.CustomUserDetails;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


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

    @Autowired
    private TeamService teamService;

    @Autowired
    private ModelMapper modelMapper;

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

    @Transactional(readOnly = true)
    public List<UserDtoResponse> getUsers(int projectId) throws ItemNotFoundException {
        Optional<Project> project = projectRepository.findById(projectId);
        if (project.isEmpty())
            throw new ItemNotFoundException(Project.class, projectId);

        Set<Short> projectTeams = teamService.getTeams().stream().map(TeamDto::getId).collect(Collectors.toSet());
        List<UserDtoResponse> usersDto = userRepository.findUsersByProjectsContainsOrderById(project.get()).stream()
                .map(user -> modelMapper.map(user, UserDtoResponse.class))
                .collect(Collectors.toList());

        // filter teams from current project
        for (UserDtoResponse userDto : usersDto) {
            if (userDto.getTeamsIds() != null)
                userDto.getTeamsIds().retainAll(projectTeams);
        }

        return usersDto;
    }


    @Transactional
    public UserDtoResponse modifyUser(UserDtoRequest userDtoRequest) throws SomeItemsNotFoundException {
        User user = userRepository.findByUsername(userDtoRequest.getUsername());

        if (userDtoRequest.getTeamsIds() != null) {
            List<Team> teams = teamRepository.findAllByDeletedAndIdIn(false, userDtoRequest.getTeamsIds());
            user.setTeams(new HashSet<>(teams));
        }

        return modelMapper.map(userRepository.save(user), UserDtoResponse.class);
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
