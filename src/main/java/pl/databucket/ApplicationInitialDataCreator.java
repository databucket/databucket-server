package pl.databucket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import pl.databucket.configuration.Constants;
import pl.databucket.dto.ColumnDto;
import pl.databucket.entity.Columns;
import pl.databucket.entity.Role;
import pl.databucket.entity.User;
import pl.databucket.entity.View;
import pl.databucket.repository.ColumnsRepository;
import pl.databucket.repository.RoleRepository;
import pl.databucket.repository.UserRepository;
import pl.databucket.repository.ViewRepository;

import java.util.*;

@Component
public class ApplicationInitialDataCreator implements ApplicationRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ColumnsRepository columnsRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private BCryptPasswordEncoder bcryptEncoder;

    public void run(ApplicationArguments args) {
        createRoles();
        createUsers();
        createColumns();
        createViews();
    }

    private void createRoles() {
        if (!roleRepository.existsByName(Constants.ROLE_SUPER)) {
            Role superAdminRole = new Role();
            superAdminRole.setName(Constants.ROLE_SUPER);
            superAdminRole.setDescription("Super admin role");
            roleRepository.save(superAdminRole);
        }

        if (!roleRepository.existsByName(Constants.ROLE_ADMIN)) {
            Role adminRole = new Role();
            adminRole.setName(Constants.ROLE_ADMIN);
            adminRole.setDescription("Project admin role");
            roleRepository.save(adminRole);
        }

        if (!roleRepository.existsByName(Constants.ROLE_MEMBER)) {
            Role userRole = new Role();
            userRole.setName(Constants.ROLE_MEMBER);
            userRole.setDescription("Project member role");
            roleRepository.save(userRole);
        }

        if (!roleRepository.existsByName(Constants.ROLE_ROBOT)) {
            Role userRole = new Role();
            userRole.setName(Constants.ROLE_ROBOT);
            userRole.setDescription("Project api client role");
            roleRepository.save(userRole);
        }
    }

    private void createUsers() {
        if (!userRepository.existsByName("super")) {
            Role superAdminRole = roleRepository.findByName(Constants.ROLE_SUPER);
            Set<Role> roles = new HashSet<>();
            roles.add(superAdminRole);

            User newUser = new User();
            newUser.setName("super");
            newUser.setPassword(bcryptEncoder.encode("super"));
            newUser = userRepository.save(newUser);

            newUser.setRoles(roles);
            userRepository.save(newUser);
        }

        if (!userRepository.existsByName("admin")) {
            User newUser = new User();
            newUser.setName("admin");
            newUser.setPassword(bcryptEncoder.encode("admin"));
            newUser = userRepository.save(newUser);

            Role adminRole = roleRepository.findByName(Constants.ROLE_ADMIN);
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

            Role memberRole = roleRepository.findByName(Constants.ROLE_MEMBER);
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

            Role memberRole = roleRepository.findByName(Constants.ROLE_ROBOT);
            Set<Role> roles = new HashSet<>();
            roles.add(memberRole);

            newUser.setRoles(roles);
            userRepository.save(newUser);
        }
    }

    private void createColumns() {

        if (!columnsRepository.existsByNameAndDeleted(Constants.COLUMNS_DEFAULT, false)) {
            List<ColumnDto> dc = new ArrayList<>();
            dc.add(new ColumnDto("Id", "data_id", "numeric", "never", true, true));
            dc.add(new ColumnDto("Tag", "tag_id", "numeric", "always", true, true));
            dc.add(new ColumnDto("Reserved", "reserved", "boolean", "always", true, true));
            dc.add(new ColumnDto("Reserved by", "reserved_by", "string", "never", true, true));
            dc.add(new ColumnDto("Created by", "created_by", "string", "never", true, true));
            dc.add(new ColumnDto("Created date", "created_date", "datetime", "never", true, true));
            dc.add(new ColumnDto("Last modified by", "last_modified_by", "string", "never", true, true));
            dc.add(new ColumnDto("Last modified date", "last_modified_date", "datetime", "never", true, true));

            Columns columns = new Columns();
            columns.setName(Constants.COLUMNS_DEFAULT);
            columns.setDescription("Default columns");
            columns.setColumns(dc);

            columnsRepository.save(columns);
        }
    }

    private void createViews() {
        if (!viewRepository.existsByNameAndDeleted(Constants.VIEWS_DEFAULT, false)) {
            View view = new View();
            Columns columns = columnsRepository.findColumnsByName(Constants.COLUMNS_DEFAULT);

            view.setName(Constants.VIEWS_DEFAULT);
            view.setDescription("Default view");
            viewRepository.save(view);

            view.setColumns(columns);
            viewRepository.save(view);
        }
    }
}
