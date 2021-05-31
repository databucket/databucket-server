package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pl.databucket.dto.AuthDtoRequest;
import pl.databucket.dto.ManageUserDtoRequest;
import pl.databucket.entity.Project;
import pl.databucket.entity.User;
import pl.databucket.exception.ItemAlreadyExistsException;
import pl.databucket.exception.SomeItemsNotFoundException;
import pl.databucket.repository.ProjectRepository;
import pl.databucket.repository.RoleRepository;
import pl.databucket.repository.UserRepository;

import java.util.HashSet;
import java.util.List;


@Service(value = "manageUserService")
public class ManageUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private BCryptPasswordEncoder bcryptEncoder;


    public List<User> getUsers() {
        return userRepository.findAllByOrderById();
    }

    public User createUser(ManageUserDtoRequest manageUserDtoRequest) throws ItemAlreadyExistsException, SomeItemsNotFoundException {
        if (userRepository.existsByUsername(manageUserDtoRequest.getUsername()))
            throw new ItemAlreadyExistsException(User.class, manageUserDtoRequest.getUsername());

        User newUser = new User();
        newUser.setUsername(manageUserDtoRequest.getUsername());
        newUser.setEnabled(manageUserDtoRequest.isEnabled());
        newUser.setExpirationDate(manageUserDtoRequest.getExpirationDate());

        if (manageUserDtoRequest.getRolesIds() != null)
            newUser.setRoles(new HashSet<>(roleRepository.findAllById(manageUserDtoRequest.getRolesIds())));

        if (manageUserDtoRequest.getProjectsIds() != null) {
            List<Project> projects = projectRepository.findAllByDeletedAndIdIn(false, manageUserDtoRequest.getProjectsIds());
            newUser.setProjects(new HashSet<>(projects));
        }

        return userRepository.save(newUser);
    }


    public User modifyUser(ManageUserDtoRequest manageUserDtoRequest) {
        User user = userRepository.findByUsername(manageUserDtoRequest.getUsername());
        user.setEnabled(manageUserDtoRequest.isEnabled());
        user.setExpirationDate(manageUserDtoRequest.getExpirationDate());

        if (manageUserDtoRequest.getRolesIds() != null)
            user.setRoles(new HashSet<>(roleRepository.findAllById(manageUserDtoRequest.getRolesIds())));
        else
            user.setRoles(null);

        if (manageUserDtoRequest.getProjectsIds() != null) {
            List<Project> projects = projectRepository.findAllByDeletedAndIdIn(false, manageUserDtoRequest.getProjectsIds());
            user.setProjects(new HashSet<>(projects));
        }

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
}
