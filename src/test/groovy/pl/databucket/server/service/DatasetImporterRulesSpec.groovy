package pl.databucket.server.service

import spock.lang.Specification

/** The tags in the rules of an imported job get the ids of the new tags, as in the filters. */
class DatasetImporterRulesSpec extends Specification {

    def "should map the tag ids in the rules of a job"() {
        given:
        def ids = new DatasetImporter.Ids()
        ids.tags.putAll([1L: 101L, 4L: 104L])
        def rules = [
                ["tagId", "==", 1],
                ["tagId", "in", [1, 4]],
                ["\$.status", "==", 4],
                [or: [["tagId", "!=", 4], ["reserved", "==", true]]],
                ["!": ["tagId", "==", 1]]
        ]

        expect:
        DatasetImporter.mapRuleTagIds(ids, rules) == [
                ["tagId", "==", 101L],
                ["tagId", "in", [101L, 104L]],
                ["\$.status", "==", 4],
                [or: [["tagId", "!=", 104L], ["reserved", "==", true]]],
                ["!": ["tagId", "==", 101L]]
        ]
        DatasetImporter.mapRuleTagIds(ids, null) == null
    }
}
