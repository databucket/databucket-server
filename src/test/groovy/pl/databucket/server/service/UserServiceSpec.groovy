package pl.databucket.server.service

import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pl.databucket.server.configuration.Constants
import pl.databucket.server.dto.ChangePasswordDtoRequest
import pl.databucket.server.dto.UserDtoRequest
import pl.databucket.server.entity.Project
import pl.databucket.server.entity.Role
import pl.databucket.server.entity.Team
import pl.databucket.server.entity.User
import pl.databucket.server.exception.ItemNotFoundException
import pl.databucket.server.repository.ProjectRepository
import pl.databucket.server.repository.TeamRepository
import pl.databucket.server.repository.UserRepository
import pl.databucket.server.security.CustomUserDetails
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.reflect.Field

class UserServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    ProjectRepository projectRepository = Mock()
    TeamRepository teamRepository = Mock()
    BCryptPasswordEncoder bcryptEncoder = Mock()

    @Subject
    UserService userService

    def setup() {
        userService = new UserService()
        setField(userService, "userRepository", userRepository)
        setField(userService, "projectRepository", projectRepository)
        setField(userService, "teamRepository", teamRepository)
        setField(userService, "bcryptEncoder", bcryptEncoder)
    }

    // ==================== loadUserByUsername Tests ====================

    def "should load user by username and create CustomUserDetails"() {
        given: "a user with roles exists"
        def adminRole = new Role(id: 1, name: "ADMIN")
        def memberRole = new Role(id: 2, name: "MEMBER")
        def user = new User(
                username: "john",
                password: "encoded-password",
                enabled: true,
                roles: [adminRole, memberRole] as Set
        )

        and: "repository returns the user"
        userRepository.findByUsername("john") >> user

        when: "loading user by username"
        def result = userService.loadUserByUsername("john")

        then: "CustomUserDetails is created correctly"
        result instanceof CustomUserDetails
        result.username == "john"
        result.password == "encoded-password"
        result.enabled == true
        result.authorities.size() == 2
        result.authorities.any { it.authority == "ROLE_ADMIN" }
        result.authorities.any { it.authority == "ROLE_MEMBER" }
    }

    def "should load superuser with correct flag"() {
        given: "a superuser exists (user with SUPER role)"
        def superRole = new Role(id: 1, name: Constants.ROLE_SUPER)
        def user = new User(
                username: "superuser",
                password: "pass",
                enabled: true,
                roles: [superRole] as Set
        )

        and: "repository returns the user"
        userRepository.findByUsername("superuser") >> user

        when: "loading superuser"
        def result = userService.loadUserByUsername("superuser")

        then: "superUser flag is set (computed from SUPER role)"
        result.superUser == true
        result.authorities.any { it.authority == "ROLE_SUPER" }
    }

    def "should handle disabled user"() {
        given: "a disabled user"
        def user = new User(
                username: "disabled-user",
                password: "pass",
                enabled: false,
                roles: [new Role(name: "MEMBER")] as Set
        )

        and: "repository returns the user"
        userRepository.findByUsername("disabled-user") >> user

        when: "loading disabled user"
        def result = userService.loadUserByUsername("disabled-user")

        then: "user is loaded but marked as disabled"
        result.enabled == false
    }

    // ==================== getUserByUsername Tests ====================

    def "should get user by username"() {
        given: "a user exists"
        def user = new User(username: "alice", email: "alice@example.com")

        and: "repository returns the user"
        userRepository.findByUsername("alice") >> user

        when: "getting user by username"
        def result = userService.getUserByUsername("alice")

        then: "correct user is returned"
        result.username == "alice"
        result.email == "alice@example.com"
    }

    def "should return null when user does not exist"() {
        given: "repository returns null"
        userRepository.findByUsername("nonexistent") >> null

        when: "getting non-existent user"
        def result = userService.getUserByUsername("nonexistent")

        then: "null is returned"
        result == null
    }

    // ==================== getUsers Tests ====================

    def "should get all users for existing project"() {
        given: "a project exists"
        def project = new Project(id: 1, name: "Project A")

        and: "users belong to this project"
        def user1 = new User(id: 1, username: "user1")
        def user2 = new User(id: 2, username: "user2")

        and: "repositories return data"
        projectRepository.findById(1) >> Optional.of(project)
        userRepository.findUsersByProjectsContainsOrderById(project) >> [user1, user2]

        when: "getting users for project"
        def result = userService.getUsers(1)

        then: "all users are returned"
        result.size() == 2
        result[0].username == "user1"
        result[1].username == "user2"
    }

    def "should throw ItemNotFoundException when project does not exist"() {
        given: "project does not exist"
        projectRepository.findById(999) >> Optional.empty()

        when: "trying to get users for non-existent project"
        userService.getUsers(999)

        then: "ItemNotFoundException is thrown"
        def exception = thrown(ItemNotFoundException)
        exception.message.contains("999")
    }

    // ==================== modifyUser Tests ====================

    def "should modify user and assign teams"() {
        given: "a user exists"
        def user = new User(username: "bob", teams: [] as Set)

        and: "teams exist"
        def team1 = new Team(id: 1, name: "Team A")
        def team2 = new Team(id: 2, name: "Team B")

        and: "user DTO with team IDs"
        def userDto = new UserDtoRequest(
                username: "bob",
                teamsIds: [1L, 2L] as Set
        )

        and: "repositories return data"
        userRepository.findByUsername("bob") >> user
        teamRepository.findAllByDeletedAndIdIn(false, [1L, 2L] as Set) >> [team1, team2]

        when: "modifying user"
        def result = userService.modifyUser(userDto)

        then: "user is updated with teams"
        result != null
        result.teams.size() == 2
        result.teams.any { it.name == "Team A" }
        result.teams.any { it.name == "Team B" }

        and: "repository save was called and returns the user"
        1 * userRepository.save(user) >> user
    }

    def "should modify user without changing teams when teamsIds is null"() {
        given: "a user exists with existing teams"
        def existingTeam = new Team(id: 1, name: "Existing Team")
        def user = new User(username: "charlie", teams: [existingTeam] as Set)

        and: "user DTO without team IDs"
        def userDto = new UserDtoRequest(username: "charlie", teamsIds: null)

        and: "repository returns user"
        userRepository.findByUsername("charlie") >> user

        when: "modifying user"
        def result = userService.modifyUser(userDto)

        then: "teams are not modified"
        result.teams.size() == 1
        0 * teamRepository.findAllByDeletedAndIdIn(_, _)

        and: "user is saved"
        1 * userRepository.save(user) >> user
    }

    // ==================== changePassword Tests ====================

    def "should change own password successfully"() {
        given: "authenticated user context"
        mockSecurityContext("alice")

        and: "user exists with current password"
        def user = new User(
                username: "alice",
                password: "encoded-old-password",
                changePassword: true
        )
        userRepository.findByUsername("alice") >> user

        and: "password change request"
        def request = new ChangePasswordDtoRequest(
                username: "alice",
                password: "old-password",
                newPassword: "new-password"
        )

        and: "password encoder matches old password and encodes new one"
        bcryptEncoder.matches("old-password", "encoded-old-password") >> true
        bcryptEncoder.encode("new-password") >> "encoded-new-password"
        userRepository.save(user) >> user

        when: "changing password"
        userService.changePassword(request)

        then: "password is changed"
        user.password == "encoded-new-password"
        user.changePassword == false
        1 * userRepository.save(user)
    }

    def "should throw exception when old password is incorrect"() {
        given: "authenticated user"
        mockSecurityContext("bob")

        and: "user exists"
        def user = new User(username: "bob", password: "encoded-password")
        userRepository.findByUsername("bob") >> user

        and: "password change request with wrong old password"
        def request = new ChangePasswordDtoRequest(
                username: "bob",
                password: "wrong-old-password",
                newPassword: "new-password"
        )

        and: "password does not match"
        bcryptEncoder.matches("wrong-old-password", "encoded-password") >> false

        when: "trying to change password"
        userService.changePassword(request)

        then: "IllegalArgumentException is thrown"
        def exception = thrown(IllegalArgumentException)
        exception.message == "Bad credentials"
        0 * userRepository.save(_)
    }

    def "should throw exception when user does not exist"() {
        given: "authenticated user changing own password"
        mockSecurityContext("nonexistent")

        and: "user does not exist (both calls return null)"
        userRepository.findByUsername("nonexistent") >> null

        and: "password change request"
        def request = new ChangePasswordDtoRequest(
                username: "nonexistent",
                password: "password",
                newPassword: "new-password"
        )

        when: "trying to change password"
        userService.changePassword(request)

        then: "IllegalArgumentException is thrown"
        def exception = thrown(IllegalArgumentException)
        exception.message == "The given user does not exist."
        0 * userRepository.save(_)
    }

    def "should allow changing ROBOT user password by another user"() {
        given: "authenticated admin user"
        mockSecurityContext("admin")

        and: "ROBOT user exists"
        def robotRole = new Role(name: Constants.ROLE_ROBOT)
        def robotUser = new User(
                username: "robot-user",
                password: "encoded-old-password",
                roles: [robotRole] as Set
        )
        userRepository.findByUsername("robot-user") >> robotUser

        and: "password change request for ROBOT user"
        def request = new ChangePasswordDtoRequest(
                username: "robot-user",
                password: "old-password",
                newPassword: "new-password"
        )

        and: "password encoder works"
        bcryptEncoder.matches("old-password", "encoded-old-password") >> true
        bcryptEncoder.encode("new-password") >> "encoded-new-password"
        userRepository.save(robotUser) >> robotUser

        when: "admin changes ROBOT password"
        userService.changePassword(request)

        then: "password is changed successfully"
        robotUser.password == "encoded-new-password"
        1 * userRepository.save(robotUser)
    }

    def "should throw exception when trying to change another non-ROBOT user password"() {
        given: "authenticated user"
        mockSecurityContext("alice")

        and: "another non-ROBOT user exists"
        def memberRole = new Role(name: Constants.ROLE_MEMBER)
        def otherUser = new User(
                username: "bob",
                password: "password",
                roles: [memberRole] as Set
        )
        userRepository.findByUsername("bob") >> otherUser

        and: "password change request for another user"
        def request = new ChangePasswordDtoRequest(
                username: "bob",
                password: "password",
                newPassword: "new-password"
        )

        when: "trying to change another user's password"
        userService.changePassword(request)

        then: "IllegalArgumentException is thrown"
        def exception = thrown(IllegalArgumentException)
        exception.message == "You cannot change the password of this user!"
        0 * bcryptEncoder.matches(_, _)
        0 * userRepository.save(_)
    }

    @Unroll
    def "should create authorities for roles: #roleNames"() {
        given: "a user with specific roles"
        def roles = roleNames.collect { new Role(name: it) } as Set
        def user = new User(
                username: "test-user",
                password: "pass",
                enabled: true,
                roles: roles
        )

        and: "repository returns the user"
        userRepository.findByUsername("test-user") >> user

        when: "loading user"
        def result = userService.loadUserByUsername("test-user")

        then: "authorities are created correctly"
        result.authorities.size() == expectedCount
        expectedAuthorities.every { expectedAuth ->
            result.authorities.any { it.authority == expectedAuth }
        }

        where: "testing different role combinations"
        roleNames                           || expectedCount | expectedAuthorities
        ["ADMIN"]                           || 1             | ["ROLE_ADMIN"]
        ["ADMIN", "MEMBER"]                 || 2             | ["ROLE_ADMIN", "ROLE_MEMBER"]
        ["SUPER", "ADMIN", "MEMBER"]        || 3             | ["ROLE_SUPER", "ROLE_ADMIN", "ROLE_MEMBER"]
        ["ROBOT"]                           || 1             | ["ROLE_ROBOT"]
    }

    // ==================== Helper Methods ====================

    private void setField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }

    private void mockSecurityContext(String username) {
        Authentication authentication = Mock()
        authentication.getName() >> username

        SecurityContext securityContext = Mock()
        securityContext.getAuthentication() >> authentication

        SecurityContextHolder.setContext(securityContext)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }
}