# Spock Framework Testing Guide

## Overview

This project uses **Spock Framework** with **Groovy** for testing. Spock provides:

- ✅ **Clear BDD-style syntax** (given-when-then)
- ✅ **Built-in mocking and stubbing**
- ✅ **Data-driven testing** with `where:` blocks
- ✅ **Better failure messages**
- ✅ **Less boilerplate** than JUnit + Mockito

## Dependencies

```gradle
// Groovy plugin
plugins {
    id 'groovy'
}

// Test dependencies
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation platform('org.spockframework:spock-bom:2.4-M4-groovy-4.0')
testImplementation 'org.spockframework:spock-core'
testImplementation 'org.spockframework:spock-spring'
testImplementation 'org.apache.groovy:groovy:4.0.24'
testRuntimeOnly 'com.h2database:h2:2.2.224'
```

## Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ExampleSpockSpec

# Run specific test method
./gradlew test --tests "ExampleSpockSpec.should demonstrate mocking"

# Run with detailed output
./gradlew test --info
```

## Test Structure

### Basic Test

```groovy
def "should add two numbers"() {
    given: "two numbers"
    def a = 5
    def b = 3
    
    when: "we add them"
    def result = a + b
    
    then: "result is correct"
    result == 8
}
```

### Data-Driven Testing

```groovy
@Unroll
def "should add #a and #b to get #expected"() {
    expect:
    a + b == expected
    
    where:
    a  | b  || expected
    1  | 2  || 3
    5  | 5  || 10
    10 | 20 || 30
}
```

### Mocking

```groovy
def "should mock repository"() {
    given: "a mock repository"
    UserRepository repo = Mock()
    
    when: "we call findByUsername"
    repo.findByUsername("john")
    
    then: "verify it was called"
    1 * repo.findByUsername("john") >> new User(username: "john")
}
```

### Exception Testing

```groovy
def "should throw exception"() {
    given:
    def service = new MyService()
    
    when:
    service.doSomethingBad()
    
    then:
    thrown(IllegalArgumentException)
}
```

## Writing Tests for Services

### Unit Test (without Spring)

```groovy
class UserServiceSpec extends Specification {
    
    UserRepository userRepository = Mock()
    UserService userService = new UserService()
    
    def setup() {
        // Inject mocks using reflection if needed
        userService.userRepository = userRepository
    }
    
    def "should return user when exists"() {
        given:
        def user = new User(username: "john")
        userRepository.findByUsername("john") >> user
        
        when:
        def result = userService.getUserByUsername("john")
        
        then:
        result.username == "john"
    }
}
```

### Integration Test (with Spring)

```groovy
@SpringBootTest
@ActiveProfiles("test")
class UserServiceIntegrationSpec extends Specification {
    
    @Autowired
    UserService userService
    
    @MockBean
    UserRepository userRepository
    
    def "should work with Spring context"() {
        given:
        userRepository.findByUsername("john") >> new User(username: "john")
        
        expect:
        userService.getUserByUsername("john").username == "john"
    }
}
```

## Best Practices

1. **Use descriptive test names** - use sentences with "should"
2. **Follow given-when-then** - makes tests readable
3. **Use @Unroll** for data-driven tests - shows each parameter combination
4. **Mock external dependencies** - keep tests fast and isolated
5. **Test behavior, not implementation** - focus on what, not how

## Example Tests

See `ExampleSpockSpec.groovy` for comprehensive examples of:
- Basic assertions
- Data-driven testing with `where:` blocks
- Mocking and stubbing
- Exception handling
- Argument matchers
- Multiple return values

## Resources

- [Spock Framework Documentation](http://spockframework.org/spock/docs/2.3/index.html)
- [Spock Primer](http://spockframework.org/spock/docs/2.3/spock_primer.html)
- [Groovy Documentation](https://groovy-lang.org/documentation.html)