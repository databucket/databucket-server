package pl.databucket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.TemplateConfDto;
import pl.databucket.server.entity.*;
import pl.databucket.server.repository.*;

import java.io.*;
import java.util.*;

@Component
public class ApplicationInitialDataCreator implements ApplicationRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private BCryptPasswordEncoder bcryptEncoder;

    public void run(ApplicationArguments args) throws IOException {
        createRoles();
        createSuperUser();
        createBaseTemplate();
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

    private void createBaseTemplate() throws IOException {
        final String NAME = "Film";
        if (!templateRepository.existsTemplateByName(NAME)) {
            File file = new ClassPathResource("exampleFilmTemplate.json").getFile();
            TemplateConfDto configuration = new ObjectMapper().readValue(file, TemplateConfDto.class);

            Template template = new Template();
            template.setName(NAME);
            template.setDescription("Example template for film data");
            template.setConfiguration(configuration);

            templateRepository.save(template);
        }
    }

}
