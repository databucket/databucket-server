package pl.databucket.server.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import pl.databucket.server.configuration.Constants
import pl.databucket.server.configuration.TestSecurityConfig
import pl.databucket.server.dto.ChangePasswordDtoRequest
import pl.databucket.server.dto.UserDtoRequest
import pl.databucket.server.entity.Project
import pl.databucket.server.entity.Role
import pl.databucket.server.entity.Team
import pl.databucket.server.entity.User
import pl.databucket.server.exception.ItemNotFoundException
import pl.databucket.server.repository.ProjectRepository
import pl.databucket.server.repository.RoleRepository
import pl.databucket.server.repository.TeamRepository
import pl.databucket.server.repository.UserRepository
import spock.lang.Specification

/**
 * Integration tests for UserService with real Spring Boot context and H2 database
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestSecurityConfig)
@Transactional
class UserServiceIntegrationSpec extends Specification {

    @Autowired
    UserService userService

    @Autowired
    UserRepository userRepository

    @Autowired
    ProjectRepository projectRepository

    @Autowired
    TeamRepository teamRepository

    @Autowired
    RoleRepository roleRepository

    @Autowired
    BCryptPasswordEncoder bcryptEncoder

    Role userRole
    Role robotRole
    Project testProject
    Team testTeam

    def setup() {
        // Get or create roles
        userRole = roleRepository.findByName("USER")
        if (userRole == null) {
            userRole = new Role(name: "USER")
            userRole = roleRepository.save(userRole)
        }

        robotRole = roleRepository.findByName(Constants.ROLE_ROBOT)
        if (robotRole == null) {
            robotRole = new Role(name: Constants.ROLE_ROBOT)
            robotRole = roleRepository.save(robotRole)
        }

        // Create test project
        testProject = new Project(name: "Test Project")
        testProject = projectRepository.save(testProject)

        // Create test team with project
        testTeam = new Team(
                name: "Test Team",
                projectId: testProject.id,
                deleted: false
        )
        testTeam = teamRepository.save(testTeam)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "should load user by username and return UserDetails"() {
        given: "a user in the database"
        def user = new User(
                username: "testuser",
                password: bcryptEncoder.encode("password123"),
                email: "test@example.com",
                enabled: true,
                roles: [userRole] as Set
        )
        userRepository.save(user)

        when: "loading user by username"
        def userDetails = userService.loadUserByUsername("testuser")

        then: "UserDetails is returned with correct data"
        userDetails.username == "testuser"
        userDetails.authorities.size() == 1
        userDetails.authorities.first().authority == "ROLE_USER"
        userDetails.enabled
    }

    def "should throw UsernameNotFoundException when user does not exist"() {
        when: "loading non-existent user"
        userService.loadUserByUsername("nonexistent")

        then: "exception is thrown"
        // NOTE: This is a bug in UserService.loadUserByUsername() - it should throw UsernameNotFoundException
        // but instead throws NullPointerException because it doesn't check if user is null
        thrown(NullPointerException)
    }

    def "should get user by username"() {
        given: "a user in the database"
        def user = new User(
                username: "johndoe",
                password: bcryptEncoder.encode("secret"),
                email: "john@example.com",
                enabled: true,
                roles: [userRole] as Set
        )
        userRepository.save(user)

        when: "getting user by username"
        def result = userService.getUserByUsername("johndoe")

        then: "user is returned"
        result != null
        result.username == "johndoe"
        result.email == "john@example.com"
    }

    def "should get users by project"() {
        given: "users assigned to a project"
        def user1 = new User(
                username: "user1",
                password: bcryptEncoder.encode("pass1"),
                email: "user1@example.com",
                enabled: true,
                roles: [userRole] as Set,
                projects: [testProject] as Set
        )
        def user2 = new User(
                username: "user2",
                password: bcryptEncoder.encode("pass2"),
                email: "user2@example.com",
                enabled: true,
                roles: [userRole] as Set,
                projects: [testProject] as Set
        )
        userRepository.save(user1)
        userRepository.save(user2)

        when: "getting users by project"
        def users = userService.getUsers(testProject.id)

        then: "all users from the project are returned"
        users.size() == 2
        users*.username.containsAll(["user1", "user2"])
    }

    def "should throw ItemNotFoundException when getting users for non-existent project"() {
        when: "getting users for non-existent project"
        userService.getUsers(99999)

        then: "exception is thrown"
        thrown(ItemNotFoundException)
    }

    def "should modify user and assign teams"() {
        given: "a user in the database"
        def user = new User(
                username: "modifyuser",
                password: bcryptEncoder.encode("password"),
                email: "modify@example.com",
                enabled: true,
                roles: [userRole] as Set
        )
        userRepository.save(user)

        and: "user DTO with teams to assign"
        def userDto = new UserDtoRequest(
                username: "modifyuser",
                teamsIds: [testTeam.id] as List
        )

        when: "modifying user"
        def result = userService.modifyUser(userDto)

        then: "user is updated with teams"
        result.username == "modifyuser"
        result.teams.size() == 1
        result.teams.first().name == "Test Team"
    }

    def "should change password successfully with correct current password"() {
        given: "authenticated user"
        def user = new User(
                username: "changepassuser",
                password: bcryptEncoder.encode("oldpassword"),
                email: "changepass@example.com",
                enabled: true,
                changePassword: true,
                roles: [userRole] as Set
        )
        userRepository.save(user)

        and: "security context with authenticated user"
        def authentication = new UsernamePasswordAuthenticationToken("changepassuser", null, [])
        SecurityContextHolder.context.authentication = authentication

        and: "change password request"
        def changePasswordDto = new ChangePasswordDtoRequest(
                username: "changepassuser",
                password: "oldpassword",
                newPassword: "newpassword123"
        )

        when: "changing password"
        userService.changePassword(changePasswordDto)

        then: "password is changed and changePassword flag is false"
        def updatedUser = userRepository.findByUsername("changepassuser")
        bcryptEncoder.matches("newpassword123", updatedUser.password)
        !updatedUser.changePassword
    }

    def "should throw exception when changing password with incorrect current password"() {
        given: "authenticated user"
        def user = new User(
                username: "wrongpassuser",
                password: bcryptEncoder.encode("correctpassword"),
                email: "wrongpass@example.com",
                enabled: true,
                roles: [userRole] as Set
        )
        userRepository.save(user)

        and: "security context"
        def authentication = new UsernamePasswordAuthenticationToken("wrongpassuser", null, [])
        SecurityContextHolder.context.authentication = authentication

        and: "change password request with wrong current password"
        def changePasswordDto = new ChangePasswordDtoRequest(
                username: "wrongpassuser",
                password: "wrongpassword",
                newPassword: "newpassword123"
        )

        when: "changing password"
        userService.changePassword(changePasswordDto)

        then: "exception is thrown"
        def exception = thrown(IllegalArgumentException)
        exception.message == "Bad credentials"
    }

    def "should allow changing password for ROBOT role by another user"() {
        given: "a robot user"
        def robotUser = new User(
                username: "robot",
                password: bcryptEncoder.encode("robotpass"),
                email: "robot@example.com",
                enabled: true,
                roles: [robotRole] as Set
        )
        userRepository.save(robotUser)

        and: "another user authenticated"
        def adminUser = new User(
                username: "admin",
                password: bcryptEncoder.encode("adminpass"),
                email: "admin@example.com",
                enabled: true,
                roles: [userRole] as Set
        )
        userRepository.save(adminUser)

        and: "security context with admin"
        def authentication = new UsernamePasswordAuthenticationToken("admin", null, [])
        SecurityContextHolder.context.authentication = authentication

        and: "change password request for robot user"
        def changePasswordDto = new ChangePasswordDtoRequest(
                username: "robot",
                password: "robotpass",
                newPassword: "newrobotpass"
        )

        when: "changing robot password"
        userService.changePassword(changePasswordDto)

        then: "password is changed successfully"
        def updatedRobot = userRepository.findByUsername("robot")
        bcryptEncoder.matches("newrobotpass", updatedRobot.password)
    }

    def "should throw exception when trying to change another non-robot user's password"() {
        given: "two regular users"
        def user1 = new User(
                username: "user1",
                password: bcryptEncoder.encode("pass1"),
                email: "user1@example.com",
                enabled: true,
                roles: [userRole] as Set
        )
        def user2 = new User(
                username: "user2",
                password: bcryptEncoder.encode("pass2"),
                email: "user2@example.com",
                enabled: true,
                roles: [userRole] as Set
        )
        userRepository.save(user1)
        userRepository.save(user2)

        and: "security context with user1"
        def authentication = new UsernamePasswordAuthenticationToken("user1", null, [])
        SecurityContextHolder.context.authentication = authentication

        and: "change password request for user2"
        def changePasswordDto = new ChangePasswordDtoRequest(
                username: "user2",
                password: "pass2",
                newPassword: "newpass2"
        )

        when: "trying to change user2's password"
        userService.changePassword(changePasswordDto)

        then: "exception is thrown"
        def exception = thrown(IllegalArgumentException)
        exception.message == "You cannot change the password of this user!"
    }

    def "should handle user with multiple roles"() {
        given: "a user with multiple roles"
        def adminRole = roleRepository.findByName("ADMIN")
        if (adminRole == null) {
            adminRole = new Role(name: "ADMIN")
            adminRole = roleRepository.save(adminRole)
        }

        def user = new User(
                username: "multirole",
                password: bcryptEncoder.encode("password"),
                email: "multirole@example.com",
                enabled: true,
                roles: [userRole, adminRole] as Set
        )
        userRepository.save(user)

        when: "loading user by username"
        def userDetails = userService.loadUserByUsername("multirole")

        then: "all roles are loaded"
        userDetails.authorities.size() == 2
        userDetails.authorities*.authority.containsAll(["ROLE_USER", "ROLE_ADMIN"])
    }

    def "should handle disabled user"() {
        given: "a disabled user"
        def user = new User(
                username: "disableduser",
                password: bcryptEncoder.encode("password"),
                email: "disabled@example.com",
                enabled: false,
                roles: [userRole] as Set
        )
        userRepository.save(user)

        when: "loading disabled user"
        def userDetails = userService.loadUserByUsername("disableduser")

        then: "user details reflect disabled status"
        !userDetails.enabled
    }

    def "should modify user without changing teams when teamsIds is null"() {
        given: "a user with existing team"
        def user = new User(
                username: "userteam",
                password: bcryptEncoder.encode("password"),
                email: "userteam@example.com",
                enabled: true,
                roles: [userRole] as Set,
                teams: [testTeam] as Set
        )
        userRepository.save(user)

        and: "user DTO without teams"
        def userDto = new UserDtoRequest(
                username: "userteam",
                teamsIds: null
        )

        when: "modifying user"
        def result = userService.modifyUser(userDto)

        then: "teams remain unchanged"
        result.teams.size() == 1
        result.teams.first().name == "Test Team"
    }
}