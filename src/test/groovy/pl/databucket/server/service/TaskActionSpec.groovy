package pl.databucket.server.service

import pl.databucket.server.dto.DataClassItemDto
import pl.databucket.server.dto.TaskConfigDto
import spock.lang.Specification
import spock.lang.Unroll

class TaskActionSpec extends Specification {

    static TaskConfigDto config(Map actions, List<Map> properties = []) {
        def config = new TaskConfigDto()
        config.actions = actions
        config.properties = properties
        config
    }

    static DataClassItemDto classProperty(String uuid, String path) {
        def item = new DataClassItemDto()
        item.uuid = uuid
        item.path = path
        item
    }

    def "should translate modify into the changes of the data API"() {
        given:
        def actions = [type      : 'modify', setTag: true, tagId: 2, setReserved: true, reserved: false,
                       properties: [[uuid: 'c1', action: 'setValue', value: 'used'],
                                    [uuid: 't1', action: 'setNull', value: 'stale'],
                                    [uuid: 'f1', action: 'remove'],
                                    [uuid: 'gone', action: 'setValue', value: 1]]]

        when:
        def action = TaskAction.of(config(actions, [[uuid: 't1', path: '$.comment']]),
                [classProperty('c1', '$.status')], [[uuid: 'f1', path: '$.phone']])

        then:
        action.type() == TaskAction.Type.MODIFY
        action.changes().tagId == 2L
        action.changes().reserved == false
        action.changes().propertiesToSet == ['$.status': 'used', '$.comment': null]
        action.changes().propertiesToRemove == ['$.phone']
    }

    def "should resolve a path defined twice by its first definition, as the dialog does"() {
        given:
        def actions = [type: 'modify', properties: [[uuid: 'c1', action: 'setValue', value: 1],
                                                     [uuid: 't1', action: 'setValue', value: 2]]]

        when:
        def action = TaskAction.of(config(actions, [[uuid: 't1', path: '$.status']]),
                [classProperty('c1', '$.status')], null)

        then: 'the task property with the same path is not used'
        action.changes().propertiesToSet == ['$.status': 1]
    }

    def "should leave out the tag and the reservation when they are not set"() {
        when:
        def action = TaskAction.of(config([type: 'modify', setTag: false, tagId: 2, setReserved: true, reserved: true]), null, null)

        then:
        action.changes().tagId == null
        action.changes().reserved == true
        action.changes().propertiesToSet == null
    }

    def "should read reserve"() {
        when:
        def action = TaskAction.of(config([type: 'reserve', number: 5, random: true, username: 'anna']), null, null)

        then:
        action.type() == TaskAction.Type.RESERVE
        action.number() == 5
        action.random()
        action.username() == 'anna'
    }

    @Unroll
    def "should read #type without changes"() {
        expect:
        TaskAction.of(config([type: type, setTag: true, tagId: 1]), null, null).type() == expected
        TaskAction.of(config([type: type, setTag: true, tagId: 1]), null, null).changes() == null

        where:
        type            || expected
        'remove'        || TaskAction.Type.REMOVE
        'clear history' || TaskAction.Type.CLEAR_HISTORY
    }

    @Unroll
    def "should refuse a task that #reason"() {
        when:
        TaskAction.of(configuration, null, null)

        then:
        def e = thrown(TaskAction.InvalidTaskException)
        e.message.contains(message)

        where:
        reason                       | configuration                                                    || message
        'has no configuration'       | null                                                             || 'no action'
        'has no action'              | config(null)                                                     || 'no action'
        'has an unknown action'      | config([type: 'archive'])                                        || 'no action'
        'changes nothing'            | config([type: 'modify', setTag: false, properties: []])         || 'changes nothing'
        'sets the tag without a tag' | config([type: 'modify', setTag: true])                           || 'no tag'
        'reserves too many rows'     | config([type: 'reserve', number: TaskAction.MAX_RESERVATION + 1]) || 'reserves'
        'reserves no rows'           | config([type: 'reserve', number: 0])                             || 'reserves'
    }
}
