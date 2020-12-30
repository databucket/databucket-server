package pl.databucket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import pl.databucket.model.constants.RoleName;
import pl.databucket.model.entity.Role;
import pl.databucket.model.entity.User;
import pl.databucket.repository.role.RoleRepository;
import pl.databucket.repository.user.UserRepository;

import java.util.HashSet;
import java.util.Set;

@Component
public class InitialDataCreator implements ApplicationRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder bcryptEncoder;

    public void run(ApplicationArguments args) {
        if (!roleRepository.existsByName(RoleName.SUPER)) {
            Role superAdminRole = new Role();
            superAdminRole.setName(RoleName.SUPER);
            superAdminRole.setDescription("Super admin role");
            roleRepository.save(superAdminRole);
        }

        if (!roleRepository.existsByName(RoleName.ADMIN)) {
            Role adminRole = new Role();
            adminRole.setName(RoleName.ADMIN);
            adminRole.setDescription("Project admin role");
            roleRepository.save(adminRole);
        }

        if (!roleRepository.existsByName(RoleName.MEMBER)) {
            Role userRole = new Role();
            userRole.setName(RoleName.MEMBER);
            userRole.setDescription("Project member role");
            roleRepository.save(userRole);
        }

        if (!roleRepository.existsByName(RoleName.ROBOT)) {
            Role userRole = new Role();
            userRole.setName(RoleName.ROBOT);
            userRole.setDescription("Project api client role");
            roleRepository.save(userRole);
        }

        if (!userRepository.existsByName("super")) {
            User newUser = new User();
            newUser.setName("super");
            newUser.setPassword(bcryptEncoder.encode("super"));
            newUser = userRepository.save(newUser);

            Role superAdminRole = roleRepository.findByName(RoleName.SUPER);
            Set<Role> roles = new HashSet<>();
            roles.add(superAdminRole);

            newUser.setRoles(roles);
            userRepository.save(newUser);
        }

        if (!userRepository.existsByName("admin")) {
            User newUser = new User();
            newUser.setName("admin");
            newUser.setPassword(bcryptEncoder.encode("admin"));
            newUser = userRepository.save(newUser);

            Role adminRole = roleRepository.findByName(RoleName.ADMIN);
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);

            newUser.setRoles(roles);
            userRepository.save(newUser);
        }

        if (!userRepository.existsByName("member")) {
            User newUser = new User();
            newUser.setName("member");
            newUser.setPassword(bcryptEncoder.encode("member"));
            newUser = userRepository.save(newUser);

            Role memberRole = roleRepository.findByName(RoleName.MEMBER);
            Set<Role> roles = new HashSet<>();
            roles.add(memberRole);

            newUser.setRoles(roles);
            userRepository.save(newUser);
        }

        if (!userRepository.existsByName("robot")) {
            User newUser = new User();
            newUser.setName("robot");
            newUser.setPassword(bcryptEncoder.encode("robot"));
            newUser = userRepository.save(newUser);

            Role memberRole = roleRepository.findByName(RoleName.ROBOT);
            Set<Role> roles = new HashSet<>();
            roles.add(memberRole);

            newUser.setRoles(roles);
            userRepository.save(newUser);
        }
    }
}
