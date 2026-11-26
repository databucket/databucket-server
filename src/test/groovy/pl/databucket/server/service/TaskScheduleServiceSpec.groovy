package pl.databucket.server.service

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import pl.databucket.server.dto.TaskScheduleDto
import pl.databucket.server.entity.Bucket
import pl.databucket.server.entity.Task
import pl.databucket.server.entity.TaskSchedule
import pl.databucket.server.entity.User
import pl.databucket.server.repository.BucketRepository
import pl.databucket.server.repository.TaskRepository
import pl.databucket.server.repository.TaskScheduleRepository
import pl.databucket.server.repository.TaskScheduleRunRepository
import spock.lang.Specification
import spock.lang.Unroll

import static pl.databucket.server.service.TaskScheduleRunner.ERROR
import static pl.databucket.server.service.TaskScheduleRunner.KEYWORD
import static pl.databucket.server.service.TaskScheduleRunner.MANUAL
import static pl.databucket.server.service.TaskScheduleRunner.OK

class TaskScheduleServiceSpec extends Specification {

    TaskScheduleRepository scheduleRepository = Mock()
    TaskRepository taskRepository = Mock()
    BucketRepository bucketRepository = Mock()
    TaskRunService taskRunService = Mock()
    TaskScheduleRunner runner = Mock()
    UserService userService = Mock()
    JobKeywordLock keywordLock = Mock()

    TaskScheduleService service = new TaskScheduleService(scheduleRepository, Mock(TaskScheduleRunRepository),
            taskRepository, bucketRepository, taskRunService, runner, userService,
            Mock(NamedParameterJdbcTemplate), keywordLock, 100)

    def setup() {
        def user = new User()
        user.username = 'robot1'
        userService.getCurrentUser() >> user
    }

    static TaskSchedule job(long id, String name, String keyword) {
        def task = new Task()
        task.name = 'clean ' + name
        def bucket = new Bucket()
        bucket.name = 'bucket-' + name
        def job = new TaskSchedule()
        job.id = id
        job.projectId = 7
        job.name = name
        job.keyword = keyword
        job.task = task
        job.bucket = bucket
        job
    }

    @Unroll
    def "should normalize the keyword '#given' to '#expected'"() {
        expect:
        TaskScheduleService.keyword(given) == expected

        where:
        given                 || expected
        null                  || null
        ''                    || null
        '   '                 || null
        ' DEV cleaning '      || 'DEV cleaning'
        'DEV   cleaning'      || 'DEV cleaning'
    }

    def "should run the jobs of the keyword by id, ignoring case, as the caller, and go on after a failure"() {
        given:
        def held = Mock(JobKeywordLock.Held)
        scheduleRepository.findAllByDeletedOrderById(false) >> [job(1, 'a', 'DEV cleaning'), job(2, 'b', 'other'),
                                                               job(3, 'c', 'dev CLEANING'), job(4, 'd', null)]

        when:
        def response = service.runKeyword('  Dev cleaning ', false)

        then:
        1 * keywordLock.tryLock(7, 'Dev cleaning') >> held
        1 * runner.runAsCaller(1, KEYWORD, 'robot1') >> new TaskScheduleRunner.Outcome(ERROR, null, 'no access')

        then:
        1 * runner.runAsCaller(3, KEYWORD, 'robot1') >> new TaskScheduleRunner.Outcome(OK, 12, 'Removed 12')
        0 * runner.runAsCaller(*_)
        1 * held.close()

        and:
        response.keyword() == 'Dev cleaning'
        !response.preview()
        response.ok() == 1
        response.errors() == 1
        response.jobs()*.id() == [1L, 3L]
        response.jobs()*.status() == [ERROR, OK]
        response.jobs()[1].count() == 12
        response.jobs()[1].task() == 'clean c'
        response.jobs()[1].bucket() == 'bucket-c'
    }

    def "should only preview the jobs of the keyword, without the lock"() {
        given:
        scheduleRepository.findAllByDeletedOrderById(false) >> [job(5, 'a', 'nightly')]

        when:
        def response = service.runKeyword('NIGHTLY', true)

        then:
        1 * runner.preview(5) >> new TaskScheduleRunner.Outcome(OK, 40, 'Would remove 40')
        0 * runner.runAsCaller(*_)
        0 * keywordLock.tryLock(*_)
        response.preview()
        response.jobs()[0].count() == 40
    }

    def "should refuse a keyword that is running"() {
        given:
        scheduleRepository.findAllByDeletedOrderById(false) >> [job(1, 'a', 'nightly')]
        keywordLock.tryLock(7, 'nightly') >> null

        when:
        service.runKeyword('nightly', false)

        then:
        thrown(TaskScheduleService.KeywordRunningException)
        0 * runner.runAsCaller(*_)
    }

    def "should list the keywords when no job has the given one"() {
        given:
        scheduleRepository.findAllByDeletedOrderById(false) >> [job(1, 'a', 'nightly'), job(2, 'b', 'DEV cleaning'),
                                                               job(3, 'c', 'Nightly')]

        when:
        service.runKeyword('dev clean', false)

        then:
        def e = thrown(TaskScheduleService.KeywordNotFoundException)
        e.message == "No job has the keyword 'dev clean'; the keywords: 'DEV cleaning', 'nightly'"
    }

    def "should ask for the keyword when it's blank"() {
        given:
        scheduleRepository.findAllByDeletedOrderById(false) >> []

        when:
        service.runKeyword(' ', false)

        then:
        def e = thrown(TaskScheduleService.InvalidScheduleException)
        e.message.startsWith('Give the keyword')
    }

    def "Run should run the job as the caller"() {
        given:
        scheduleRepository.findByIdAndDeleted(9, false) >> job(9, 'a', null)

        when:
        service.runNow(9)

        then:
        1 * runner.runAsCaller(9, MANUAL, 'robot1')
    }

    def "should save a job without a cron: no next run, the keyword trimmed"() {
        given:
        def task = new Task()
        task.id = 3
        task.name = 'clean'
        def bucket = new Bucket()
        bucket.id = 4
        bucket.name = 'orders'
        taskRepository.findByIdAndDeleted(3, false) >> task
        bucketRepository.findByIdAndDeleted(4, false) >> bucket
        scheduleRepository.save(_) >> { TaskSchedule saved -> saved }
        def dto = new TaskScheduleDto()
        dto.name = 'Clean orders'
        dto.taskId = 3
        dto.bucketId = 4
        dto.cron = cron
        dto.keyword = ' DEV  cleaning '
        dto.enabled = true

        when:
        def saved = service.createSchedule(dto)

        then:
        1 * taskRunService.run('orders', '3', _, true)
        saved.cron == null
        saved.keyword == 'DEV cleaning'
        saved.nextRunAt == null
        saved.runAs == 'robot1'

        where:
        cron << [null, '', '  ']
    }

    def "should keep the properties of the rules builder: #given -> #expected"() {
        given:
        def task = new Task(); task.id = 3
        def bucket = new Bucket(); bucket.id = 4; bucket.name = 'orders'
        taskRepository.findByIdAndDeleted(3, false) >> task
        bucketRepository.findByIdAndDeleted(4, false) >> bucket
        scheduleRepository.save(_) >> { TaskSchedule saved -> saved }
        def dto = new TaskScheduleDto(name: 'Clean orders', taskId: 3, bucketId: 4, properties: given)

        when:
        def saved = service.createSchedule(dto)

        then:
        saved.properties == expected

        where:
        given                                                              || expected
        null                                                               || null
        []                                                                 || null
        [[uuid: 'u1', path: '$.city', title: 'City', type: 'string']]      || [[uuid: 'u1', path: '$.city', title: 'City', type: 'string']]
    }

    def "should import a job of a dataset disabled, without a preview"() {
        given:
        def task = new Task(); task.id = 3
        def bucket = new Bucket(); bucket.id = 4; bucket.name = 'orders'
        taskRepository.findByIdAndDeleted(3, false) >> task
        bucketRepository.findByIdAndDeleted(4, false) >> bucket
        scheduleRepository.save(_) >> { TaskSchedule saved -> saved }
        def job = new pl.databucket.server.dto.DatasetJobDto(name: ' Clean ', taskId: 3, bucketId: 4,
                rules: [["id", ">", 0]], cron: '0 2 * * *', timeZone: 'Europe/Warsaw', keyword: ' DEV  cleaning ')

        when:
        def saved = service.importJob(job)

        then:
        0 * taskRunService.run(*_)
        saved.name == 'Clean'
        saved.task.is(task)
        saved.bucket.is(bucket)
        saved.rules == [["id", ">", 0]]
        saved.cron == '0 0 2 * * *'
        saved.timeZone == 'Europe/Warsaw'
        saved.keyword == 'DEV cleaning'
        !saved.enabled
        saved.nextRunAt == null
        saved.runAs == 'robot1'
    }

    def "should save a job with a cron: the next run set"() {
        given:
        def task = new Task()
        task.id = 3
        def bucket = new Bucket()
        bucket.id = 4
        bucket.name = 'orders'
        taskRepository.findByIdAndDeleted(3, false) >> task
        bucketRepository.findByIdAndDeleted(4, false) >> bucket
        scheduleRepository.save(_) >> { TaskSchedule saved -> saved }
        def dto = new TaskScheduleDto()
        dto.name = 'Clean orders'
        dto.taskId = 3
        dto.bucketId = 4
        dto.cron = '0 2 * * *'
        dto.timeZone = 'UTC'
        dto.enabled = true

        when:
        def saved = service.createSchedule(dto)

        then:
        saved.cron == '0 0 2 * * *'
        saved.keyword == null
        saved.nextRunAt != null
    }
}
