package pl.databucket.server.service

import pl.databucket.server.dto.ManageUserDtoRequest
import pl.databucket.server.entity.Project
import pl.databucket.server.entity.Role
import pl.databucket.server.entity.User
import pl.databucket.server.exception.ItemAlreadyExistsException
import pl.databucket.server.repository.ProjectRepository
import pl.databucket.server.repository.RoleRepository
import pl.databucket.server.repository.UserRepository
import org.springframework.security.authentication.AccountExpiredException
import pl.databucket.server.security.TokenProvider
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.reflect.Field

class ManageUserServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    RoleRepository roleRepository = Mock()
    ProjectRepository projectRepository = Mock()

    @Subject
    ManageUserService manageUserService

    def setup() {
        manageUserService = new ManageUserService()
        setField(manageUserService, "userRepository", userRepository)
        setField(manageUserService, "roleRepository", roleRepository)
        setField(manageUserService, "projectRepository", projectRepository)
    }

    // ==================== getUsers Tests ====================

    def "should list the users with their top role and projects, loaded in one query"() {
        given: "a user with two roles (from before 3.12.0) and two projects"
        def member = new Role(); member.id = 3 as short; member.name = "MEMBER"
        def admin = new Role(); admin.id = 2 as short; admin.name = "ADMIN"
        def p1 = new Project(); p1.id = 10
        def p2 = new Project(); p2.id = 11
        def user = new User()
        user.id = 7; user.username = "anna"; user.email = "anna@example.com"; user.description = "QA"
        user.enabled = true; user.roles = [member, admin] as Set; user.projects = [p1, p2] as Set
        user.createdBy = "super"; user.modifiedBy = "admin"

        when:
        def users = manageUserService.getUsers()

        then:
        1 * userRepository.findAllWithProjectsAndRolesByOrderById() >> [user]
        0 * userRepository.findAllByOrderById()
        users.size() == 1
        with(users[0]) {
            id == 7
            username == "anna"
            email == "anna@example.com"
            description == "QA"
            enabled
            rolesIds == [2 as short] as Set
            projectsIds == [10, 11] as Set
            createdBy == "super"
            modifiedBy == "admin"
        }
    }

    def "should list a user without roles and projects with null ids"() {
        given:
        def user = new User(); user.id = 8; user.username = "robot"; user.roles = [] as Set; user.projects = [] as Set

        when:
        def users = manageUserService.getUsers()

        then:
        1 * userRepository.findAllWithProjectsAndRolesByOrderById() >> [user]
        users[0].rolesIds == null
        users[0].projectsIds == null
    }

    // ==================== createUser Tests ====================

    def "should reject creating a user with an email already in use"() {
        given: "another user already has this email in a different case"
        userRepository.existsByUsername("john") >> false
        userRepository.existsByEmailIgnoreCase("john@example.com") >> true

        when: "creating the user"
        manageUserService.createUser(new ManageUserDtoRequest(username: "john", email: " John@Example.com "))

        then: "the duplicate is rejected and nothing is saved"
        def e = thrown(ItemAlreadyExistsException)
        e.message.contains("email 'john@example.com'")
        0 * userRepository.save(_)
    }

    def "should store a new user's email normalized"() {
        given: "the email is free"
        userRepository.existsByUsername("john") >> false
        userRepository.existsByEmailIgnoreCase("john@example.com") >> false

        when: "creating the user"
        manageUserService.createUser(new ManageUserDtoRequest(username: "john", email: " John@Example.com "))

        then: "the email is saved trimmed and lower-cased"
        1 * userRepository.save({ it.email == "john@example.com" }) >> { User u -> u }
    }

    def "should create users without email without checking uniqueness"() {
        given: "the username is free"
        userRepository.existsByUsername("john") >> false

        when: "creating a user with a blank email"
        manageUserService.createUser(new ManageUserDtoRequest(username: "john", email: "  "))

        then: "no email lookup is made and the email is stored as null"
        0 * userRepository.existsByEmailIgnoreCase(_)
        1 * userRepository.save({ it.email == null }) >> { User u -> u }
    }

    // ==================== modifyUser Tests ====================

    def "should reject changing a user's email to one used by another user"() {
        given: "an existing user"
        def user = new User(id: 7, username: "john", email: "john@example.com")
        userRepository.findByUsername("john") >> user
        userRepository.existsByEmailIgnoreCaseAndIdNot("anna@example.com", 7) >> true

        when: "changing the email to another user's"
        manageUserService.modifyUser(new ManageUserDtoRequest(username: "john", email: "Anna@example.com"))

        then: "the change is rejected and the user is untouched"
        thrown(ItemAlreadyExistsException)
        user.email == "john@example.com"
        0 * userRepository.save(_)
    }

    @Unroll
    def "should allow keeping own email when given as '#email'"() {
        given: "an existing user"
        def user = new User(id: 7, username: "john", email: "john@example.com")
        user.lastSendEmailForgotPasswordLinkDate = new Date()
        userRepository.findByUsername("john") >> user
        userRepository.existsByEmailIgnoreCaseAndIdNot("john@example.com", 7) >> false

        when: "saving the user with the same email"
        manageUserService.modifyUser(new ManageUserDtoRequest(username: "john", email: email))

        then: "the email stays and the mail throttling dates are kept"
        1 * userRepository.save(user) >> user
        user.email == "john@example.com"
        user.lastSendEmailForgotPasswordLinkDate != null

        where:
        email << ["john@example.com", "John@Example.com", " john@example.com "]
    }

    // ==================== User.normalizeEmail Tests ====================

    @Unroll
    def "should normalize email '#input' to '#expected'"() {
        expect:
        User.normalizeEmail(input) == expected

        where:
        input                 || expected
        null                  || null
        ""                    || null
        "   "                 || null
        "John@Example.COM"    || "john@example.com"
        "  john@example.com " || "john@example.com"
    }

    // ==================== confirmation links ====================

    @Unroll
    def "should refuse a sign-up confirmation link that #reason"() {
        given:
        TokenProvider tokenProvider = Mock()
        setField(manageUserService, "jwtTokenUtil", tokenProvider)
        tokenProvider.isTokenExpired("link") >> expired
        tokenProvider.unpackFromJwts("link") >> "john@example.com"
        userRepository.findByEmailIgnoreCase("john@example.com") >> null

        when:
        manageUserService.signUpUserConfirmation("link")

        then:
        def e = thrown(AccountExpiredException)
        e.message == "The confirmation link is invalid or expired!"
        0 * userRepository.save(_)

        where:
        reason                          | expired
        "is expired or invalid"         | true
        "belongs to a removed user"     | false
    }

    @Unroll
    def "should refuse a forgot-password confirmation link that #reason"() {
        given:
        TokenProvider tokenProvider = Mock()
        setField(manageUserService, "jwtTokenUtil", tokenProvider)
        tokenProvider.isTokenExpired("link") >> expired
        tokenProvider.unpackFromJwts("link") >> "john@example.com"
        userRepository.findByEmailIgnoreCase("john@example.com") >> null

        when:
        manageUserService.resetAndSendPassword("link")

        then:
        thrown(AccountExpiredException)
        0 * userRepository.save(_)

        where:
        reason                          | expired
        "is expired or invalid"         | true
        "belongs to a removed user"     | false
    }

    private void setField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }
}
