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
import pl.databucket.dto.UserDtoRequest;
import pl.databucket.entity.Role;
import pl.databucket.entity.User;
import pl.databucket.repository.RoleRepository;
import pl.databucket.repository.UserRepository;
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
                user.getEnabled());
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

    public User findByName(String name) {
        return userRepository.findByName(name);
    }

    public User save(UserDtoRequest userDto) {
        if (!userRepository.existsByName(userDto.getName())) {
            User newUser = new User();
            newUser.setName(userDto.getName());
            newUser.setPassword(bcryptEncoder.encode(userDto.getPassword()));
            return userRepository.save(newUser);
        } else
            throw new IllegalArgumentException("The given user already exists.");
    }

    public User addRole(RoleDto roleDto) {
        User user = userRepository.findByName(roleDto.getUserName());
        if (user == null)
            throw new IllegalArgumentException("The given user has not been found!");

        Role role = roleRepository.findByName(roleDto.getRoleName());
        if (role == null)
            throw new IllegalArgumentException("The given role has not been found!");

        Set<Role> roles = user.getRoles();
        if (roles == null)
            roles = new HashSet<>();
        roles.add(role);
        return userRepository.save(user);
    }

    public User removeRole(RoleDto roleDto) {
        User user = userRepository.findByName(roleDto.getUserName());
        if (user == null)
            throw new IllegalArgumentException("The given user has not been found!");

        Role role = roleRepository.findByName(roleDto.getRoleName());
        if (role == null)
            throw new IllegalArgumentException("The given role has not been found!");

        Set<Role> roles = user.getRoles();
        if (roles == null)
            return user;

        roles.remove(role);
        return userRepository.save(user);
    }

    public User resetPassword(UserDtoRequest userDto) {
        User user = userRepository.findByName(userDto.getName());
        if (user != null) {
            user.setPassword(bcryptEncoder.encode(userDto.getPassword()));
            user.setChangePassword(true);
            return userRepository.save(user);
        } else
            throw new IllegalArgumentException("The given user does not exist.");
    }

    public User changePassword(UserDtoRequest userDto) {
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
