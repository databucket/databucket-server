package pl.databucket.server.service

import pl.databucket.server.entity.Project
import pl.databucket.server.exception.ProjectNotEmptyException
import pl.databucket.server.repository.ProjectRepository
import pl.databucket.server.repository.UserRepository
import spock.lang.Specification
import spock.lang.Subject

class ManageProjectServiceSpec extends Specification {

    ProjectRepository projectRepository = Mock()
    UserRepository userRepository = Mock()
    ProjectContentService projectContentService = Mock()

    @Subject
    ManageProjectService service = new ManageProjectService()

    def setup() {
        service.projectRepository = projectRepository
        service.userRepository = userRepository
        service.projectContentService = projectContentService
    }

    def "should refuse to delete a project that is not empty"() {
        given:
        projectRepository.findByIdAndDeleted(5, false) >> new Project(id: 5, name: "demo", users: [] as Set)
        projectContentService.isEmpty(5) >> false

        when:
        service.deleteProject(5)

        then:
        def e = thrown(ProjectNotEmptyException)
        e.message.contains("Clear it before deleting it")
        0 * projectContentService.clearProject(_)
        0 * projectRepository.save(_)
    }

    def "should delete an empty project and remove what is left of objects deleted before"() {
        given:
        def project = new Project(id: 5, name: "demo", users: [] as Set)
        projectRepository.findByIdAndDeleted(5, false) >> project
        projectContentService.isEmpty(5) >> true

        when:
        service.deleteProject(5)

        then:
        1 * projectContentService.clearProject(5)
        1 * projectRepository.save({ it.deleted })
    }
}
