package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pl.databucket.server.configuration.AppProperties;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.AuthReqDTO;
import pl.databucket.server.dto.ForgotPasswordReqDTO;
import pl.databucket.server.dto.ManageUserDtoRequest;
import pl.databucket.server.dto.SignUpDtoRequest;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.Role;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ForbiddenRepetitionException;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.repository.ProjectRepository;
import pl.databucket.server.repository.RoleRepository;
import pl.databucket.server.repository.UserRepository;
import pl.databucket.server.security.TokenProvider;
import pl.databucket.server.service.mail.MailSenderService;

import javax.mail.MessagingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;


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

    @Autowired
    private TokenProvider jwtTokenUtil;

    @Autowired
    private MailSenderService mailSenderService;

    @Autowired
    private AppProperties appProperties;

    public List<User> getUsers() {
        return userRepository.findAllByOrderById();
    }

    public User createUser(ManageUserDtoRequest manageUserDtoRequest) throws ItemAlreadyExistsException, SomeItemsNotFoundException {
        if (userRepository.existsByUsername(manageUserDtoRequest.getUsername()))
            throw new ItemAlreadyExistsException(User.class, manageUserDtoRequest.getUsername());

        User newUser = new User();
        newUser.setUsername(manageUserDtoRequest.getUsername());
        newUser.setEmail(manageUserDtoRequest.getEmail());
        newUser.setDescription(manageUserDtoRequest.getDescription());
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

    public void signUpUser(SignUpDtoRequest signUpDtoRequest) throws MessagingException {
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName(Constants.ROLE_MEMBER));

        User newUser = new User();
        newUser.setUsername(signUpDtoRequest.getUsername());
        newUser.setEmail(signUpDtoRequest.getEmail());
        newUser.setPassword(bcryptEncoder.encode(signUpDtoRequest.getPassword()));
        newUser.setEnabled(false);
        newUser.setChangePassword(false);
        newUser.setRoles(roles);
        newUser = userRepository.save(newUser);

        mailSenderService.sendConfirmationLink(newUser, signUpDtoRequest.getUrl() + jwtTokenUtil.packToJwts(signUpDtoRequest.getEmail()), appProperties.getMailFrom());
    }

    public void signUpUserConfirmation(String jwts) throws ForbiddenRepetitionException, MessagingException {
        if (!jwtTokenUtil.isTokenExpired(jwts)) {
            String email = jwtTokenUtil.unpackFromJwts(jwts);
            User user = userRepository.findByEmail(email);

            if (user.getEnabled())
                throw new ForbiddenRepetitionException("The user has been already activated");

            user.setEnabled(true);
            userRepository.save(user);

            mailSenderService.sendRegistrationConfirmation(user, appProperties.getMailFrom());
        } else
            throw new AccountExpiredException("The confirmation link is expired!");
    }

    public void forgotPasswordMessage(ForgotPasswordReqDTO forgotPasswordReqDTO) throws ForbiddenRepetitionException, MessagingException, UsernameNotFoundException {
        if (!userRepository.existsByEmail(forgotPasswordReqDTO.getEmail()))
            throw new UsernameNotFoundException("User not found! Make sure you have entered the correct email address.");

        User user = userRepository.findByEmail(forgotPasswordReqDTO.getEmail());

        if (user.getLastSendEmailForgotPasswordLinkDate() != null) {
            Instant lastSendEmailTime = user.getLastSendEmailForgotPasswordLinkDate().toInstant();
            Instant currentTime = Instant.now();

            if (ChronoUnit.HOURS.between(lastSendEmailTime, currentTime) < 48)
                throw new ForbiddenRepetitionException("The confirmation link has been send within last 48 hours. Search it in your email inbox.");
        }

        mailSenderService.sendForgotPasswordLink(user, forgotPasswordReqDTO.getUrl() + jwtTokenUtil.packToJwts(forgotPasswordReqDTO.getEmail()), appProperties.getMailFrom());
        user.setLastSendEmailForgotPasswordLinkDate(new Date());
        userRepository.save(user);
    }

    public User modifyUser(ManageUserDtoRequest manageUserDtoRequest) {
        User user = userRepository.findByUsername(manageUserDtoRequest.getUsername());
        user.setEmail(manageUserDtoRequest.getEmail());
        user.setDescription(manageUserDtoRequest.getDescription());
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

    public void resetAndSendPassword(AuthReqDTO userDto) {
        User user = userRepository.findByUsername(userDto.getUsername());
        if (user != null) {
            user.setPassword(bcryptEncoder.encode(userDto.getPassword()));
            user.setChangePassword(true);
            userRepository.save(user);
        } else
            throw new IllegalArgumentException("The given user does not exist.");
    }

    public void resetAndSendPassword(String jwts) throws ForbiddenRepetitionException, MessagingException {
        if (!jwtTokenUtil.isTokenExpired(jwts)) {
            String email = jwtTokenUtil.unpackFromJwts(jwts);
            User user = userRepository.findByEmail(email);

            if (user.getLastSendEmailTempPasswordDate() != null) {
                Instant lastSendEmailTime = user.getLastSendEmailTempPasswordDate().toInstant();
                Instant currentTime = Instant.now();

                if (ChronoUnit.HOURS.between(lastSendEmailTime, currentTime) < 48)
                    throw new ForbiddenRepetitionException("The temporary password has been send within last 48 hours. Search it in your email inbox.");
            }

            int length = 11;
            String small_letter = "abcdefghijklmnopqrstuvwxyz";
            String numbers = "0123456789";

            String finalString = small_letter + numbers;
            Random random = new Random();
            char[] password = new char[length];
            for (int i = 0; i < length; i++)
                password[i] = finalString.charAt(random.nextInt(finalString.length()));

            String newPassword = new String(password);

            mailSenderService.sendNewPassword(user, newPassword, appProperties.getMailFrom());

            user.setPassword(bcryptEncoder.encode(newPassword));
            user.setChangePassword(true);
            user.setLastSendEmailTempPasswordDate(new Date());
            userRepository.save(user);
        } else
            throw new AccountExpiredException("The confirmation link is expired!");
    }
}
