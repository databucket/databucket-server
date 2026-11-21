package pl.databucket.server.service.data.generator

import pl.databucket.server.dto.DataCreateDTO
import pl.databucket.server.dto.DataGenerateDTO
import pl.databucket.server.entity.Bucket
import pl.databucket.server.entity.User
import pl.databucket.server.service.data.DataService
import spock.lang.Specification
import spock.lang.Unroll

class DataGenerateServiceSpec extends Specification {

    def dataService = Mock(DataService)
    def service = new DataGenerateService(dataService)
    def user = new User()
    def bucket = new Bucket()

    private static DataGenerateDTO request(Map props) {
        def request = new DataGenerateDTO()
        request.fields = ['$.email': 'user-{n}@example.com']
        props.each { k, v -> request."$k" = v }
        return request
    }

    def "should insert the rows with the tag and reservation, numbered after the last id"() {
        given:
        List<DataCreateDTO> inserted = null

        when:
        def result = service.generate(user, bucket, request(count: 5, tagId: 3L, reserved: true))

        then:
        1 * dataService.lastDataId(bucket) >> 40
        1 * dataService.createData(user, bucket, _ as List) >> { args -> inserted = args[2] }
        inserted.size() == 5
        inserted*.properties*.email == (41..45).collect { "user-$it@example.com" }
        inserted.every { it.tagId == 3L && it.reserved }
        result.inserted() == 5
        result.firstN() == 41
        result.lastN() == 45
        result.rows()*.email == ['user-41@example.com', 'user-42@example.com', 'user-43@example.com']
    }

    def "should start {n} from startN without reading the last id"() {
        when:
        def result = service.generate(user, bucket, request(count: 2, startN: 100L))

        then:
        0 * dataService.lastDataId(_)
        1 * dataService.createData(user, bucket, { it.size() == 2 })
        result.firstN() == 100
        result.lastN() == 101
    }

    def "should preview the first rows without inserting them"() {
        when:
        def result = service.preview(bucket, request(count: count))

        then:
        1 * dataService.lastDataId(bucket) >> 0
        0 * dataService.createData(*_)
        result.inserted() == 0
        result.rows().size() == rows
        result.firstN() == 1
        result.lastN() == rows

        where:
        count | rows
        null  | DataGenerateService.PREVIEW_ROWS
        3     | 3
        5000  | DataGenerateService.PREVIEW_ROWS
    }

    def "should preview the same rows that a generate with the same seed inserts"() {
        given:
        def fields = ['$.name': '#{Name.firstName}', '$.age': [number: [18, 90]]]
        List<DataCreateDTO> inserted = null
        dataService.createData(user, bucket, _ as List) >> { args -> inserted = args[2] }

        when:
        def preview = service.preview(bucket, request(fields: fields, count: 20, seed: 7L, startN: 1L))
        service.generate(user, bucket, request(fields: fields, count: 20, seed: 7L, startN: 1L))

        then:
        preview.rows() == inserted.take(DataGenerateService.PREVIEW_ROWS)*.properties
    }

    @Unroll
    def "should refuse count #count"() {
        when:
        service.generate(user, bucket, request(count: count))

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('1..10000')
        0 * dataService.createData(*_)

        where:
        count << [null, 0, -1, 10_001]
    }

    def "should refuse an invalid generator before inserting anything"() {
        when:
        service.generate(user, bucket, request(fields: [:], count: 10))

        then:
        thrown(IllegalArgumentException)
        0 * dataService.createData(*_)
    }
}
