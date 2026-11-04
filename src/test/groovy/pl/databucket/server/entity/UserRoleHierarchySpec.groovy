package pl.databucket.server.entity

import spock.lang.Specification
import spock.lang.Unroll

/**
 * One role per user, SUPER > ADMIN > MEMBER > ROBOT; older accounts with several roles count their highest.
 */
class UserRoleHierarchySpec extends Specification {

    @Unroll
    def "should take #top as the role of a user with #names"() {
        given:
        def user = userWith(names)

        expect:
        user.topRole?.name == top
        user.rolesIds == (top == null ? null : [ids[top]] as Set)

        where:
        names                           || top
        ["MEMBER"]                      || "MEMBER"
        ["ADMIN", "MEMBER", "ROBOT"]    || "ADMIN"
        ["ROBOT", "SUPER"]              || "SUPER"
        ["UNKNOWN"]                     || null
        []                              || null
    }

    @Unroll
    def "should give #role the roles #implied"() {
        expect:
        userWith([role]).impliedRoleNames == implied

        where:
        role     || implied
        "SUPER"  || ["SUPER", "ADMIN", "MEMBER", "ROBOT"]
        "ADMIN"  || ["ADMIN", "MEMBER", "ROBOT"]
        "MEMBER" || ["MEMBER", "ROBOT"]
        "ROBOT"  || ["ROBOT"]
    }

    @Unroll
    def "should #text a #userRole user to a bucket with the role #bucketRole"() {
        expect: "the role of a bucket, view or group means that role or higher"
        userWith([userRole]).hasRoleAtLeast(new Role(name: bucketRole)) == allowed

        where:
        userRole | bucketRole || allowed
        "SUPER"  | "ADMIN"    || true
        "ADMIN"  | "ADMIN"    || true
        "MEMBER" | "ADMIN"    || false
        "ADMIN"  | "MEMBER"   || true
        "MEMBER" | "MEMBER"   || true
        "ROBOT"  | "MEMBER"   || false
        "MEMBER" | "ROBOT"    || true

        text = allowed ? "let" : "not let"
    }

    def "should tell SUPER and ADMIN users"() {
        expect:
        userWith(["SUPER"]).superUser
        userWith(["SUPER"]).adminUser
        !userWith(["ADMIN"]).superUser
        userWith(["ADMIN"]).adminUser
        !userWith(["MEMBER"]).adminUser
        !new User(roles: null).adminUser
    }

    private static final Map<String, Short> ids = [SUPER: 1 as short, ADMIN: 2 as short, MEMBER: 3 as short, ROBOT: 4 as short, UNKNOWN: 9 as short]

    private static User userWith(List<String> names) {
        new User(roles: names.collect { new Role(id: ids[it], name: it) } as Set)
    }
}
