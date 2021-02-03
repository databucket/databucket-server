package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pl.databucket.configuration.Constants;
import pl.databucket.dto.*;
import pl.databucket.entity.*;
import pl.databucket.exception.ItemAlreadyExistsException;
import pl.databucket.exception.SomeItemsNotFoundException;
import pl.databucket.repository.*;
import pl.databucket.security.CustomUserDetails;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service(value = "userService")
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

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

    public Page<User> getUsers(Specification<User> specification, Pageable pageable) {
        return userRepository.findAll(specification, pageable);
    }

    public List<Role> getRoles() {
        return roleRepository.findAllByOrderByIdAsc();
    }

    public void delete(long id) {
        userRepository.deleteById(id);
    }

    public User createUser(UserDtoRequest userDtoRequest) throws ItemAlreadyExistsException, SomeItemsNotFoundException {
        if (userRepository.existsByUsername(userDtoRequest.getUsername()))
            throw new ItemAlreadyExistsException(User.class, userDtoRequest.getUsername());

        User newUser = new User();
        newUser.setUsername(userDtoRequest.getUsername());
        newUser.setEnabled(userDtoRequest.isEnabled());
        newUser.setExpirationDate(userDtoRequest.getExpirationDate());

        if (userDtoRequest.getRolesIds() != null)
            newUser.setRoles(new HashSet<>(roleRepository.findAllById(userDtoRequest.getRolesIds())));

        if (userDtoRequest.getProjectsIds() != null) {
            List<Project> projects = projectRepository.findAllByDeletedAndIdIn(false, userDtoRequest.getProjectsIds());
            if (userDtoRequest.getProjectsIds().size() != projects.size()) {
                List<Long> givenIds = userDtoRequest.getProjectsIds().stream().mapToLong(Integer::longValue).boxed().collect(Collectors.toList());
                List<Integer> foundIds = projects.stream().map(Project::getId).collect(Collectors.toList());
                List<Long> foundIdsLong = foundIds.stream().mapToLong(Integer::longValue).boxed().collect(Collectors.toList());
                givenIds.removeAll(foundIdsLong);
                throw new SomeItemsNotFoundException(Project.class, givenIds);
            } else
                newUser.setProjects(new HashSet<>(projects));
        }

        return userRepository.save(newUser);
    }

    public User createUserFull(UserDtoRequest userDtoRequest) throws ItemAlreadyExistsException, SomeItemsNotFoundException {
        if (userRepository.existsByUsername(userDtoRequest.getUsername()))
            throw new ItemAlreadyExistsException(User.class, userDtoRequest.getUsername());

        User newUser = new User();
        newUser.setUsername(userDtoRequest.getUsername());
        newUser.setPassword(bcryptEncoder.encode(userDtoRequest.getPassword()));
        newUser.setEnabled(userDtoRequest.isEnabled());
        newUser.setExpirationDate(userDtoRequest.getExpirationDate());

        if (userDtoRequest.getRolesIds() != null)
            newUser.setRoles(new HashSet<>(roleRepository.findAllById(userDtoRequest.getRolesIds())));

        if (userDtoRequest.getProjectsIds() != null) {
            List<Project> projects = projectRepository.findAllByDeletedAndIdIn(false, userDtoRequest.getProjectsIds());
            if (userDtoRequest.getProjectsIds().size() != projects.size()) {
                List<Long> givenIds = userDtoRequest.getProjectsIds().stream().mapToLong(Integer::longValue).boxed().collect(Collectors.toList());
                List<Integer> foundIds = projects.stream().map(Project::getId).collect(Collectors.toList());
                List<Long> foundIdsLong = foundIds.stream().mapToLong(Integer::longValue).boxed().collect(Collectors.toList());
                givenIds.removeAll(foundIdsLong);
                throw new SomeItemsNotFoundException(Project.class, givenIds);
            } else
                newUser.setProjects(new HashSet<>(projects));
        }

        if (userDtoRequest.getGroupsIds() != null) {
            List<Group> groups = groupRepository.findAllByDeletedAndIdIn(false, userDtoRequest.getGroupsIds());
            if (userDtoRequest.getGroupsIds().size() != groups.size()) {
                List<Long> givenIds = userDtoRequest.getGroupsIds().stream().mapToLong(Long::longValue).boxed().collect(Collectors.toList());
                List<Long> foundIds = groups.stream().map(Group::getId).collect(Collectors.toList());
                givenIds.removeAll(foundIds);
                throw new SomeItemsNotFoundException(Group.class, givenIds);
            } else
                newUser.setGroups(new HashSet<>(groups));
        }

        if (userDtoRequest.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, userDtoRequest.getBucketsIds());
            if (userDtoRequest.getBucketsIds().size() != buckets.size()) {
                List<Long> givenIds = userDtoRequest.getBucketsIds().stream().mapToLong(Long::longValue).boxed().collect(Collectors.toList());
                List<Long> foundIds = buckets.stream().map(Bucket::getId).collect(Collectors.toList());
                givenIds.removeAll(foundIds);
                throw new SomeItemsNotFoundException(Bucket.class, givenIds);
            } else
                newUser.setBuckets(new HashSet<>(buckets));
        }
        return userRepository.save(newUser);
    }

    public User modifyUser(UserDtoRequest userDto) throws SomeItemsNotFoundException {

        User user = userRepository.findByUsername(userDto.getUsername());
        user.setEnabled(userDto.isEnabled());
        user.setExpirationDate(userDto.getExpirationDate());

        if (userDto.getRolesIds() != null)
            user.setRoles(new HashSet<>(roleRepository.findAllById(userDto.getRolesIds())));
        else
            user.setRoles(null);

        if (userDto.getProjectsIds() != null) {
            List<Project> projects = projectRepository.findAllByDeletedAndIdIn(false, userDto.getProjectsIds());
            if (userDto.getProjectsIds().size() != projects.size()) {
                List<Long> givenIds = userDto.getProjectsIds().stream().mapToLong(Integer::longValue).boxed().collect(Collectors.toList());
                List<Integer> foundIds = projects.stream().map(Project::getId).collect(Collectors.toList());
                List<Long> foundIdsLong = foundIds.stream().mapToLong(Integer::longValue).boxed().collect(Collectors.toList());
                givenIds.removeAll(foundIdsLong);
                throw new SomeItemsNotFoundException(Project.class, givenIds);
            } else
                user.setProjects(new HashSet<>(projects));
        } else
            user.setProjects(null);

        if (userDto.getGroupsIds() != null) {
            List<Group> groups = groupRepository.findAllByDeletedAndIdIn(false, userDto.getGroupsIds());
            if (userDto.getGroupsIds().size() != groups.size()) {
                List<Long> givenIds = userDto.getGroupsIds().stream().mapToLong(Long::longValue).boxed().collect(Collectors.toList());
                List<Long> foundIds = groups.stream().map(Group::getId).collect(Collectors.toList());
                givenIds.removeAll(foundIds);
                throw new SomeItemsNotFoundException(Group.class, givenIds);
            } else
                user.setGroups(new HashSet<>(groups));
        } else
            user.setGroups(null);

        if (userDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, userDto.getBucketsIds());
            if (userDto.getBucketsIds().size() != buckets.size()) {
                List<Long> givenIds = userDto.getBucketsIds().stream().mapToLong(Long::longValue).boxed().collect(Collectors.toList());
                List<Long> foundIds = buckets.stream().map(Bucket::getId).collect(Collectors.toList());
                givenIds.removeAll(foundIds);
                throw new SomeItemsNotFoundException(Bucket.class, givenIds);
            } else
                user.setBuckets(new HashSet<>(buckets));
        } else
            user.setBuckets(null);

        return userRepository.save(user);
    }

    public void resetPassword(AuthDtoRequest userDto) {
        User user = userRepository.findByUsername(userDto.getUsername());
        if (user != null) {
            user.setPassword(bcryptEncoder.encode(userDto.getPassword()));
            user.setChangePassword(true);
            userRepository.save(user);
        } else
            throw new IllegalArgumentException("The given user does not exist.");
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
