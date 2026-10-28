package pl.databucket.server.service

import pl.databucket.server.configuration.ApplicationConfiguration
import pl.databucket.server.dto.ManageUserDtoRequest
import pl.databucket.server.entity.User
import pl.databucket.server.exception.ItemAlreadyExistsException
import pl.databucket.server.repository.ProjectRepository
import pl.databucket.server.repository.RoleRepository
import pl.databucket.server.repository.UserRepository
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
        setField(manageUserService, "modelMapper", new ApplicationConfiguration().modelMapper())
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

    private void setField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }
}
