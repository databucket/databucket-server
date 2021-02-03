package pl.databucket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import pl.databucket.configuration.Constants;
import pl.databucket.entity.*;
import pl.databucket.repository.*;

import java.util.*;

@Component
public class ApplicationInitialDataCreator implements ApplicationRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder bcryptEncoder;

    public void run(ApplicationArguments args) {
        createRoles();
        createSuperUser();
    }

    private void createRoles() {
        if (!roleRepository.existsByName(Constants.ROLE_SUPER)) {
            Role superAdminRole = new Role();
            superAdminRole.setName(Constants.ROLE_SUPER);
            roleRepository.save(superAdminRole);
        }

        if (!roleRepository.existsByName(Constants.ROLE_ADMIN)) {
            Role adminRole = new Role();
            adminRole.setName(Constants.ROLE_ADMIN);
            roleRepository.save(adminRole);
        }

        if (!roleRepository.existsByName(Constants.ROLE_MEMBER)) {
            Role userRole = new Role();
            userRole.setName(Constants.ROLE_MEMBER);
            roleRepository.save(userRole);
        }

        if (!roleRepository.existsByName(Constants.ROLE_ROBOT)) {
            Role userRole = new Role();
            userRole.setName(Constants.ROLE_ROBOT);
            roleRepository.save(userRole);
        }
    }

    private void createSuperUser() {
        if (!userRepository.existsByUsername("super")) {
            Role superAdminRole = roleRepository.findByName(Constants.ROLE_SUPER);
            Set<Role> roles = new HashSet<>();
            roles.add(superAdminRole);

            User newUser = new User();
            newUser.setUsername("super");
            newUser.setPassword(bcryptEncoder.encode("super"));
            newUser.setRoles(roles);
            userRepository.save(newUser);
        }
    }

    private void createColumns() {
//        if (!columnsRepository.existsByNameAndDeleted(Constants.COLUMNS_DEFAULT, false)) {
//            List<ColumnDto> dc = new ArrayList<>();
//            dc.add(new ColumnDto("Id", "data_id", "numeric", "never", true, true));
//            dc.add(new ColumnDto("Tag", "tag_id", "numeric", "always", true, true));
//            dc.add(new ColumnDto("Reserved", "reserved", "boolean", "always", true, true));
//            dc.add(new ColumnDto("Reserved by", "reserved_by", "string", "never", true, true));
//            dc.add(new ColumnDto("Created by", "created_by", "string", "never", true, true));
//            dc.add(new ColumnDto("Created date", "created_date", "datetime", "never", true, true));
//            dc.add(new ColumnDto("Last modified by", "last_modified_by", "string", "never", true, true));
//            dc.add(new ColumnDto("Last modified date", "last_modified_date", "datetime", "never", true, true));
//
//            DataColumns columns = new DataColumns();
//            columns.setName(Constants.COLUMNS_DEFAULT);
//            columns.setDescription("Default columns");
//            columns.setColumns(dc);
//
//            columnsRepository.save(columns);
//        }
    }

    private void createViews() {
//        if (!viewRepository.existsByNameAndDeleted(Constants.VIEWS_DEFAULT, false)) {
//            View view = new View();
//            DataColumns columns = columnsRepository.findColumnsByName(Constants.COLUMNS_DEFAULT);
//
//            view.setName(Constants.VIEWS_DEFAULT);
//            view.setDescription("Default view");
//            viewRepository.save(view);
//
//            view.setDataColumns(columns);
//            viewRepository.save(view);
//        }
    }
}
