package pl.databucket.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import pl.databucket.server.dto.DataCreateDTO
import pl.databucket.server.dto.DatasetDto
import pl.databucket.server.dto.SvgDto
import pl.databucket.server.dto.TeamDto
import spock.lang.Specification

class DatasetWriterSpec extends Specification {

    ObjectMapper objectMapper = new ObjectMapper()
    DatasetWriter writer = new DatasetWriter(objectMapper)

    DatasetDto demo() {
        objectMapper.readValue(DatasetWriterSpec.getResourceAsStream("/datasets/demo.json"), DatasetDto)
    }

    def "should write the DEMO dataset so that it reads back the same"() {
        given:
        DatasetDto original = demo()

        when:
        String text = writer.write(original)
        DatasetDto read = objectMapper.readValue(text, DatasetDto)

        then:
        objectMapper.valueToTree(read) == objectMapper.valueToTree(demo())
        writer.write(read) == text
    }

    def "should write one data row per line"() {
        given:
        DatasetDto dataset = demo()
        int rows = dataset.data.values()*.size().sum() as int

        when:
        List<String> lines = writer.write(dataset).readLines()

        then:
        lines.count { it.startsWith('      {"tagId":') } == rows
        lines.any { it == '  "data": {' }
        lines.last() == '}'
    }

    def "should leave out users and audit fields, but not in data properties"() {
        given:
        DatasetDto dataset = new DatasetDto(name: "X")
        dataset.teams = [new TeamDto(id: 1 as Short, name: "team", usersIds: [5L] as Set, createdBy: "admin", createdAt: new Date())]
        dataset.icons = [new SvgDto(id: 1L, name: "star", structure: "<path d='M1 1'/>", createdBy: "admin", modifiedAt: new Date())]
        dataset.data = [bucket: [new DataCreateDTO(tagId: null, reserved: false, properties: [createdBy: "someone"])]]

        when:
        String text = writer.write(dataset)

        then:
        !text.contains('"usersIds"')
        !text.contains('"admin"')
        !text.contains('"createdAt"')
        !text.contains('"modifiedAt"')
        text.contains('"structure": "<path d=\'M1 1\'/>"')
        text.contains('{"tagId":null,"reserved":false,"properties":{"createdBy":"someone"}}')
    }

    def "should write a dataset without data"() {
        when:
        String text = writer.write(new DatasetDto(name: "Empty"))

        then:
        text.contains('"data": {}')
        text.contains('"teams": []')
        objectMapper.readValue(text, DatasetDto).name == "Empty"
    }
}
