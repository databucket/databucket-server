package pl.databucket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import pl.databucket.configuration.Constants;
import pl.databucket.dto.ColumnDto;
import pl.databucket.entity.*;
import pl.databucket.repository.*;

import java.util.*;

@Component
public class ApplicationInitialDataCreator implements ApplicationRunner {

//    @Autowired
//    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

//    @Autowired
//    private DataColumnsRepository columnsRepository;

//    @Autowired
//    private ViewRepository viewRepository;


    @Autowired
    private BCryptPasswordEncoder bcryptEncoder;

    public void run(ApplicationArguments args) {
//        createProjects();
        createRoles();
        createUsers();
//        createColumns();
//        createViews();
    }

//    private void createProjects() {
//
//        if (!projectRepository.existsByName(Constants.PROJECT_DEMO_NAME1)) {
//            Project project = new Project();
//            project.setName(Constants.PROJECT_DEMO_NAME1);
//            project.setDescription(Constants.PROJECT_DEMO_DESCRIPTION1);
//            projectRepository.save(project);
//        }
//
//        if (!projectRepository.existsByName(Constants.PROJECT_DEMO_NAME2)) {
//            Project project = new Project();
//            project.setName(Constants.PROJECT_DEMO_NAME2);
//            project.setDescription(Constants.PROJECT_DEMO_DESCRIPTION2);
//            projectRepository.save(project);
//        }
//    }

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

    private void createUsers() {
//        Project project1 = projectRepository.findByName(Constants.PROJECT_DEMO_NAME1);
//        Project project2 = projectRepository.findByName(Constants.PROJECT_DEMO_NAME2);
//        Set<Project> oneProject = new HashSet<>();
//        oneProject.add(project1);
//        Set<Project> twoProjects = new HashSet<>();
//        twoProjects.add(project1);
//        twoProjects.add(project2);

        if (!userRepository.existsByName("super")) {
            Role superAdminRole = roleRepository.findByName(Constants.ROLE_SUPER);
            Set<Role> roles = new HashSet<>();
            roles.add(superAdminRole);

            User newUser = new User();
            newUser.setName("super");
            newUser.setPassword(bcryptEncoder.encode("super"));
            newUser.setRoles(roles);
//            newUser.setProjects(twoProjects);
            userRepository.save(newUser);
        }
//
//        if (!userRepository.existsByName("admin")) {
//            Role adminRole = roleRepository.findByName(Constants.ROLE_ADMIN);
//            Set<Role> roles = new HashSet<>();
//            roles.add(adminRole);
//
//            User newUser = new User();
//            newUser.setName("admin");
//            newUser.setPassword(bcryptEncoder.encode("admin"));
//            newUser.setRoles(roles);
//            newUser.setProjects(twoProjects);
//            userRepository.save(newUser);
//        }

//        if (!userRepository.existsByName("member")) {
//            Role memberRole = roleRepository.findByName(Constants.ROLE_MEMBER);
//            Set<Role> roles = new HashSet<>();
//            roles.add(memberRole);
//
//            User newUser = new User();
//            newUser.setName("member");
//            newUser.setPassword(bcryptEncoder.encode("member"));
//            newUser.setRoles(roles);
//            newUser.setProjects(oneProject);
//            userRepository.save(newUser);
//        }

//        if (!userRepository.existsByName("robot")) {
//            Role memberRole = roleRepository.findByName(Constants.ROLE_ROBOT);
//            Set<Role> roles = new HashSet<>();
//            roles.add(memberRole);
//
//            User newUser = new User();
//            newUser.setName("robot");
//            newUser.setPassword(bcryptEncoder.encode("robot"));
//            newUser.setRoles(roles);
//            userRepository.save(newUser);
//        }
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
