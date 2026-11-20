package pl.databucket.server.mcp

import spock.lang.Specification
import spock.lang.Unroll

class BucketSchemaSpec extends Specification {

    def "should guess the fields of an example row"() {
        when:
        def result = BucketSchema.of([login   : 'anna', age: 34, balance: 12.5, active: true, birthDate: '1990-05-01',
                                      lastLogin: '2025-01-02T10:11:12Z', opensAt: '08:30', comment: null,
                                      address : [city: 'Paris', postCode: '75001'], roles: ['a'], extra: [:]], null)

        then:
        result.fields().collect { [it.path(), it.title(), it.type()] } == [
                ['$.login', 'Login', 'string'],
                ['$.age', 'Age', 'numeric'],
                ['$.balance', 'Balance', 'numeric'],
                ['$.active', 'Active', 'boolean'],
                ['$.birthDate', 'Birth date', 'date'],
                ['$.lastLogin', 'Last login', 'datetime'],
                ['$.opensAt', 'Opens at', 'time'],
                ['$.comment', 'Comment', 'string'],
                ['$.address.city', 'City', 'string'],
                ['$.address.postCode', 'Post code', 'string'],
        ]
        result.skipped().size() == 2
        result.skipped()[0].startsWith('$.roles')
    }

    def "should let the definitions change and add fields"() {
        when:
        def result = BucketSchema.of([status: 'active', age: 1], [
                [path: '$.status', values: ['active', 'blocked', 'active']],
                [path: 'age', title: 'Age in years'],
                [path: '$.score', type: 'number'],
                [path: '$.level', type: 'enum', values: [1, 2, 3]],
        ])

        then:
        result.fields().collect { [it.path(), it.title(), it.type(), it.values()] } == [
                ['$.status', 'Status', 'select', ['active', 'blocked']],
                ['$.age', 'Age in years', 'numeric', null],
                ['$.score', 'Score', 'numeric', null],
                ['$.level', 'Level', 'select', ['1', '2', '3']],
        ]
    }

    @Unroll
    def "should refuse: #reason"() {
        when:
        BucketSchema.of(sample, fields)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains(message)

        where:
        reason             | sample           | fields                                           | message
        'nothing'          | null             | null                                             | 'Give the fields'
        'no path'          | null             | [[type: 'string']]                               | 'needs a path'
        'unknown item'     | null             | [[path: '$.a', format: 'x']]                     | "unknown item 'format'"
        'bad type'         | null             | [[path: '$.a', type: 'money']]                   | 'the type is one of'
        'enum, no values'  | null             | [[path: '$.a', type: 'enum']]                    | 'needs its values'
        'values, not enum' | null             | [[path: '$.a', type: 'string', values: ['x']]]   | 'only an enum'
        'empty values'     | null             | [[path: '$.a', values: []]]                      | 'values is a list'
        'bad path'         | null             | [[path: '$.a(b)']]                               | 'not a usable property path'
        'bad key'          | ['a.b': 1]       | null                                             | 'not a usable property path'
        'overlap'          | [a: [b: 1]]      | [[path: '$.a']]                                  | 'overlap'
    }

    def "should make names unique within 30 characters"() {
        expect:
        McpBucketCreator.unique('orders', { it in taken }) == expected

        where:
        taken                    | expected
        []                       | 'orders'
        ['orders']               | 'orders 2'
        ['orders', 'orders 2']   | 'orders 3'
    }

    def "should cut a long name to 30 characters"() {
        expect:
        McpBucketCreator.unique('a' * 40, { false }) == 'a' * 30
        McpBucketCreator.unique('a' * 40, { it == 'a' * 30 }) == 'a' * 28 + ' 2'
    }
}
