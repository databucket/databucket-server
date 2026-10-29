package pl.databucket.server.security

import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import pl.databucket.server.configuration.ApplicationConfiguration
import pl.databucket.server.configuration.Constants
import pl.databucket.server.entity.Project
import pl.databucket.server.entity.Role
import pl.databucket.server.entity.User
import spock.lang.Specification
import spock.lang.Subject

import java.lang.reflect.Field

class AuthResponseBuilderSpec extends Specification {

    TokenProvider tokenProvider = Mock()
    def authentication = new UsernamePasswordAuthenticationToken("john", null, [])

    @Subject
    AuthResponseBuilder builder

    def setup() {
        builder = new AuthResponseBuilder()
        setField(builder, "jwtTokenUtil", tokenProvider)
        setField(builder, "modelMapper", new ApplicationConfiguration().modelMapper())
    }

    def "should sign a user with one project into it"() {
        given:
        def user = user([project(1)])

        when:
        def response = builder.build(user, authentication, null, true, "auth.signin")

        then:
        1 * tokenProvider.generateToken(authentication, 1) >> "token-1"
        response.statusCode == HttpStatus.OK
        response.body.token == "token-1"
        response.body.username == "john"
        response.body.project.id == 1
        response.body.roles == [Constants.ROLE_MEMBER]
    }

    def "should list the projects of a user with several projects without issuing a token"() {
        given:
        def user = user([project(1), project(2)])

        when:
        def response = builder.build(user, authentication, null, false, "auth.sso-signin")

        then:
        0 * tokenProvider.generateToken(*_)
        response.statusCode == HttpStatus.OK
        response.body.token == null
        response.body.projects*.id.sort() == [1, 2]
    }

    def "should list the projects sorted by name ignoring case"() {
        given:
        def user = user([project(1, "demo2"), project(2, "Alpha"), project(3, "demo1")])

        when:
        def response = builder.build(user, authentication, null, false, "auth.sso-signin")

        then:
        response.body.projects*.name == ["Alpha", "demo1", "demo2"]
    }

    def "should sign into the chosen project"() {
        given:
        def user = user([project(1), project(2)])

        when:
        def response = builder.build(user, authentication, 2, false, "auth.sso-signin")

        then:
        1 * tokenProvider.generateToken(authentication, 2) >> "token-2"
        response.body.token == "token-2"
        response.body.project.id == 2
    }

    def "should refuse a project the user is not assigned to"() {
        when:
        def response = builder.build(user([project(1)]), authentication, 9, false, "auth.sso-signin")

        then:
        0 * tokenProvider.generateToken(*_)
        response.statusCode == HttpStatus.FORBIDDEN
        response.body.message == "The user is not assigned to given project!"
    }

    def "should enforce a pending password change only for a password login"() {
        given:
        def user = user([project(1)])
        user.changePassword = true

        when: "signing in with the password"
        def passwordResponse = builder.build(user, authentication, null, true, "auth.signin")

        then: "the password change is required"
        1 * tokenProvider.generateToken(authentication, 0) >> "token-0"
        passwordResponse.body.changePassword

        when: "signing in through SSO"
        def ssoResponse = builder.build(user, authentication, null, false, "auth.sso-signin")

        then: "the user goes straight to the project"
        1 * tokenProvider.generateToken(authentication, 1) >> "token-1"
        ssoResponse.body.changePassword == null
        ssoResponse.body.project.id == 1
    }

    def "should refuse an expired user"() {
        given:
        def user = user([project(1)])
        user.expirationDate = new Date(System.currentTimeMillis() - 1000)

        when:
        def response = builder.build(user, authentication, null, false, "auth.sso-signin")

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        response.body.message == "User access has expired!"
    }

    private static User user(List<Project> projects) {
        new User(username: "john", enabled: true, changePassword: false,
                roles: [new Role(id: 2, name: Constants.ROLE_MEMBER)] as Set,
                projects: projects as Set)
    }

    private static Project project(int id, String name = "project-" + id) {
        new Project(id: id, name: name, enabled: true)
    }

    private static void setField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }
}
