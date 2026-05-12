package pl.databucket.server

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Example Spock test demonstrating core features:
 * - Given-When-Then structure
 * - Mocking and Stubbing
 * - Data-driven testing
 * - Exception handling
 */
class ExampleSpockSpec extends Specification {

    def "should demonstrate basic Spock test structure"() {
        given: "we have two numbers"
        def a = 10
        def b = 5

        when: "we add them"
        def result = a + b

        then: "the result should be correct"
        result == 15
        result > 0
        result instanceof Integer
    }

    @Unroll
    def "should add #a and #b to get #expected"() {
        expect: "addition works correctly"
        a + b == expected

        where: "testing multiple combinations"
        a  | b  || expected
        1  | 2  || 3
        5  | 5  || 10
        -1 | 1  || 0
        10 | 20 || 30
    }

    def "should demonstrate mocking"() {
        given: "a mocked list"
        List mockList = Mock()

        when: "we call methods on the mock"
        mockList.add("item")
        def size = mockList.size()

        then: "we can verify interactions"
        1 * mockList.add("item")  // verify add was called once with "item"
        1 * mockList.size() >> 1  // verify size was called and return 1
        size == 1
    }

    def "should demonstrate stubbing with multiple return values"() {
        given: "a stubbed list that returns different values"
        List stubbedList = Stub()
        stubbedList.size() >>> [1, 2, 3]

        expect: "each call returns next value"
        stubbedList.size() == 1
        stubbedList.size() == 2
        stubbedList.size() == 3
    }

    def "should handle exceptions"() {
        given: "a map that throws exception"
        Map map = Mock()
        map.get("key") >> { throw new IllegalArgumentException("Not found") }

        when: "we try to get a value"
        map.get("key")

        then: "exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "should demonstrate complex stubbing"() {
        given: "a calculator service mock"
        def calculator = Mock(SimpleCalculator)

        and: "stubbing with argument matching"
        calculator.add(5, 3) >> 8
        calculator.add(10, 20) >> 30
        calculator.multiply(_, _) >> { int a, int b -> a * b }

        expect: "stubbed methods work correctly"
        calculator.add(5, 3) == 8
        calculator.add(10, 20) == 30
        calculator.multiply(4, 5) == 20
    }

    @Unroll
    def "should validate string '#input' has length #expectedLength"() {
        expect:
        input.length() == expectedLength
        input.isEmpty() == empty

        where:
        input   || expectedLength | empty
        ""      || 0              | true
        "test"  || 4              | false
        "hello" || 5              | false
    }

    // Helper class for demonstration
    interface SimpleCalculator {
        int add(int a, int b)
        int multiply(int a, int b)
    }
}