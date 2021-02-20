package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pl.databucket.configuration.Constants;
import pl.databucket.dto.ChangePasswordDtoRequest;
import pl.databucket.dto.UserDtoRequest;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.Group;
import pl.databucket.entity.Project;
import pl.databucket.entity.User;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.SomeItemsNotFoundException;
import pl.databucket.repository.BucketRepository;
import pl.databucket.repository.GroupRepository;
import pl.databucket.repository.ProjectRepository;
import pl.databucket.repository.UserRepository;
import pl.databucket.security.CustomUserDetails;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service(value = "userService")
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private BucketRepository bucketRepository;

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
                user.getBucketsIds(),
                user.getEnabled(),
                user.isSuperUser());
    }

    private Set<SimpleGrantedAuthority> getAuthority(User user) {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
        });
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

        if (userDtoRequest.getGroupsIds() != null) {
            List<Group> groups = groupRepository.findAllByDeletedAndIdIn(false, userDtoRequest.getGroupsIds());
            user.setGroups(new HashSet<>(groups));
        }

        if (userDtoRequest.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, userDtoRequest.getBucketsIds());
            user.setBuckets(new HashSet<>(buckets));
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
