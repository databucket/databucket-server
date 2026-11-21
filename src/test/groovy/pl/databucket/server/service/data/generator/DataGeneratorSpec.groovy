package pl.databucket.server.service.data.generator

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant
import java.time.LocalDate

class DataGeneratorSpec extends Specification {

    def "should build nested properties from the paths"() {
        given:
        def generator = DataGenerator.of(['$.name': 'John', 'address.city': 'Paris', '$.address.zip': 12345,
                                          '$.active': true, '$.comment': null], null, 1L)

        when:
        def row = generator.next(1)

        then:
        row == [name: 'John', address: [city: 'Paris', zip: 12345], active: true, comment: null]
    }

    def "should put the row number in place of {n}"() {
        given:
        def generator = DataGenerator.of(['$.email': 'user-{n}@example.com'], null, null)

        when:
        def rows = generator.next(41, 3)

        then:
        rows*.email == ['user-41@example.com', 'user-42@example.com', 'user-43@example.com']
    }

    def "should evaluate Datafaker expressions, also with {n}"() {
        given:
        def generator = DataGenerator.of(['$.name': '#{Name.firstName}', '$.code': '{n}-#{numerify \'###\'}'], 'pl', 7L)

        when:
        def row = generator.next(5)

        then:
        row.name instanceof String && !row.name.isBlank() && !row.name.contains('#{')
        row.code ==~ /5-\d{3}/
    }

    def "should give the same rows for the same seed"() {
        given:
        def spec = ['$.name'  : '#{Name.fullName}', '$.age': [number: [18, 90]],
                    '$.status': [oneOf: ['a', 'b', 'c']], '$.day': [date: ['2024-01-01', '2024-12-31']]]

        expect:
        DataGenerator.of(spec, 'en', 42L).next(1, 20) == DataGenerator.of(spec, 'en', 42L).next(1, 20)
    }

    def "should keep numbers in the range with the decimals"() {
        given:
        def generator = DataGenerator.of(['$.age': [number: [18, 20]], '$.balance': [number: [0, 10], decimals: 2]],
                null, 3L)

        when:
        def rows = generator.next(1, 500)

        then:
        rows*.age.every { it instanceof Long && it >= 18 && it <= 20 }
        rows*.age.toSet() == [18L, 19L, 20L] as Set
        rows*.balance.every { it instanceof BigDecimal && it.scale() == 2 && it >= 0 && it <= 10 }
    }

    def "should choose by the weights"() {
        given:
        def generator = DataGenerator.of(['$.status': [oneOf: ['active', 'blocked', 'never'], weights: [9, 1, 0]]],
                null, 5L)

        when:
        def statuses = generator.next(1, 1000)*.status

        then:
        statuses.count('never') == 0
        statuses.count('active') > 800
        statuses.count('blocked') > 50
    }

    def "should keep dates and times in the range"() {
        given:
        def generator = DataGenerator.of(['$.day' : [date: ['2024-02-27', '2024-03-02']],
                                          '$.time': [datetime: ['2024-01-01', '2024-01-01T00:00:10Z']]], null, 9L)

        when:
        def rows = generator.next(1, 200)

        then:
        rows*.day.every { def d = LocalDate.parse(it); !d.isBefore(LocalDate.parse('2024-02-27')) && !d.isAfter(LocalDate.parse('2024-03-02')) }
        rows*.day.toSet().size() == 5
        rows*.time.every { def t = Instant.parse(it); t >= Instant.parse('2024-01-01T00:00:00Z') && t <= Instant.parse('2024-01-01T00:00:10Z') }
    }

    @Unroll
    def "should refuse a wrong spec: #reason"() {
        when:
        DataGenerator.of(spec, null, null)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains(message)

        where:
        reason               | spec                                                   | message
        'no fields'          | [:]                                                    | 'at least one field'
        'bad path'           | ['$.a..b': 1]                                          | 'not a property path'
        'overlapping paths'  | ['$.a': 1, '$.a.b': 2]                                 | 'overlap'
        'list'               | ['$.a': [1, 2]]                                        | 'a list isn\'t a generator'
        'unknown object'     | ['$.a': [foo: 1]]                                      | 'exactly one of'
        'two kinds'          | ['$.a': [number: [1, 2], date: ['2024-01-01', '2024-01-02']]] | 'exactly one of'
        'extra key'          | ['$.a': [number: [1, 2], weights: [1]]]                | "'weights' doesn't go with 'number'"
        'min > max'          | ['$.a': [number: [5, 1]]]                              | 'min is greater than max'
        'no whole number'    | ['$.a': [number: [0.1, 0.9]]]                          | 'no whole number'
        'bad decimals'       | ['$.a': [number: [1, 2], decimals: 20]]                | 'decimals is 0..10'
        'empty oneOf'        | ['$.a': [oneOf: []]]                                   | 'non-empty list'
        'weights size'       | ['$.a': [oneOf: ['x', 'y'], weights: [1]]]             | 'one per value'
        'zero weights'       | ['$.a': [oneOf: ['x'], weights: [0]]]                  | 'above 0'
        'bad date'           | ['$.a': [date: ['2024-13-01', '2024-12-31']]]          | 'yyyy-MM-dd'
        'dates reversed'     | ['$.a': [date: ['2024-12-31', '2024-01-01']]]          | 'after the second'
        'bad datetime'       | ['$.a': [datetime: ['yesterday', '2024-12-31']]]       | 'ISO 8601'
    }

    @Unroll
    def "should say which field has an expression Datafaker doesn't know: #text"() {
        given:
        def generator = DataGenerator.of(['$.x': text], null, null)

        when:
        generator.next(7)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.startsWith("Field '\$.x': Datafaker doesn't know $unknown.")
        e.message.contains('#{Name.firstName}')
        e.message.contains(DataGenerator.PROVIDERS_URL)
        !e.message.contains('FakerContext')

        where:
        text                         | unknown
        '#{NoSuchProvider.foo}'      | '#{NoSuchProvider.foo}'
        '#{Name.noSuchMethod}'       | '#{Name.noSuchMethod}'
        'id-{n} #{Name.nope}'        | '#{Name.nope}'
    }

    def "should refuse an expression without its closing brace"() {
        when:
        DataGenerator.of(['$.x': 'user #{Name.firstName'], null, null)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "Field '\$.x': the expression #{Name.firstName has no closing }, e.g. #{Name.firstName}"
    }
}
