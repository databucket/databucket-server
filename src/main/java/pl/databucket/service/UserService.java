package pl.databucket.service;

import org.modelmapper.ModelMapper;
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
import pl.databucket.dto.AuthProjectDto;
import pl.databucket.dto.RoleDto;
import pl.databucket.dto.AuthDtoRequest;
import pl.databucket.dto.UserDtoRequest;
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
    private ModelMapper modelMapper;

    @Autowired
    private BCryptPasswordEncoder bcryptEncoder;

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException("Invalid username or password.");
        }

        Integer projectId = null;
        List<AuthProjectDto> projects = null;
        int projectsCount = (user.getProjects() != null ? user.getProjects().size() : 0);
        if (projectsCount == 1)
            projectId = user.getProjects().iterator().next().getId();
        else if (projectsCount > 0)
            projects = user.getProjects().stream().map(item -> modelMapper.map(item, AuthProjectDto.class)).collect(Collectors.toList());

        return new CustomUserDetails(
                user.getName(),
                user.getPassword(),
                getAuthority(user),
                projectId,
                projects,
                user.isChangePassword(),
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

    public void delete(long id) {
        userRepository.deleteById(id);
    }

    public User createUser(UserDtoRequest userDto) throws ItemAlreadyExistsException, SomeItemsNotFoundException {
        if (userRepository.existsByName(userDto.getName()))
            throw new ItemAlreadyExistsException(User.class, userDto.getName());

        User newUser = new User();
        newUser.setName(userDto.getName());
        newUser.setPassword(bcryptEncoder.encode(userDto.getPassword()));

        if (userDto.getRolesIds() != null)
            newUser.setRoles(new HashSet<>(roleRepository.findAllById(userDto.getRolesIds())));

        if (userDto.getProjectsIds() != null) {
            List<Project> projects = projectRepository.findAllByDeletedAndIdIn(false, userDto.getProjectsIds());
            if (userDto.getProjectsIds().size() != projects.size()) {
                List<Long> givenIds = userDto.getProjectsIds().stream().mapToLong(Integer::longValue).boxed().collect(Collectors.toList());
                List<Integer> foundIds = projects.stream().map(Project::getId).collect(Collectors.toList());
                List<Long> foundIdsLong = foundIds.stream().mapToLong(Integer::longValue).boxed().collect(Collectors.toList());
                givenIds.removeAll(foundIds);
                throw new SomeItemsNotFoundException(Project.class, givenIds);
            } else
                newUser.setProjects(new HashSet<>(projects));
        }

        if (userDto.getGroupsIds() != null) {
            List<Group> groups = groupRepository.findAllByDeletedAndIdIn(false, userDto.getGroupsIds());
            if (userDto.getGroupsIds().size() != groups.size()) {
                List<Long> givenIds = userDto.getGroupsIds().stream().mapToLong(Long::longValue).boxed().collect(Collectors.toList());
                List<Long> foundIds = groups.stream().map(Group::getId).collect(Collectors.toList());
                givenIds.removeAll(foundIds);
                throw new SomeItemsNotFoundException(Group.class, givenIds);
            } else
                newUser.setGroups(new HashSet<>(groups));
        }

        if (userDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, userDto.getBucketsIds());
            if (userDto.getBucketsIds().size() != buckets.size()) {
                List<Long> givenIds = userDto.getBucketsIds().stream().mapToLong(Long::longValue).boxed().collect(Collectors.toList());
                List<Long> foundIds = buckets.stream().map(Bucket::getId).collect(Collectors.toList());
                givenIds.removeAll(foundIds);
                throw new SomeItemsNotFoundException(Bucket.class, givenIds);
            } else
                newUser.setBuckets(new HashSet<>(buckets));
        }
        return userRepository.save(newUser);
    }

    public User modifyUser(UserDtoRequest userDto) throws SomeItemsNotFoundException {

        User user = userRepository.findByName(userDto.getName());

        user.setEnabled(userDto.getEnabled());

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

    public User resetPassword(AuthDtoRequest userDto) {
        User user = userRepository.findByName(userDto.getName());
        if (user != null) {
            user.setPassword(bcryptEncoder.encode(userDto.getPassword()));
            user.setChangePassword(true);
            return userRepository.save(user);
        } else
            throw new IllegalArgumentException("The given user does not exist.");
    }

    public User changePassword(AuthDtoRequest userDto) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!name.equals(userDto.getName())) {
            User user = userRepository.findByName(userDto.getName());
            if (user.getRoles().size() == 1 && user.getRoles().stream().noneMatch(role -> role.getName().equals(Constants.ROLE_ROBOT)))
                throw new IllegalArgumentException("You cannot change the password of another user!");
        }

        User user = userRepository.findByName(userDto.getName());
        if (user != null) {
            user.setPassword(bcryptEncoder.encode(userDto.getPassword()));
            user.setChangePassword(false);
            return userRepository.save(user);
        } else
            throw new IllegalArgumentException("The given user does not exist.");
    }
}
