package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.ManageProjectDto;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.Template;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.ProjectRepository;
import pl.databucket.server.repository.TemplateRepository;
import pl.databucket.server.repository.UserRepository;

import java.util.HashSet;
import java.util.List;


@Service
public class ManageProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TemplateRepository templateRepository;

    public Project createProject(ManageProjectDto manageProjectDto) {
        Project project = new Project();
        project.setName(manageProjectDto.getName());
        project.setDescription(manageProjectDto.getDescription());
        project.setExpirationDate(manageProjectDto.getExpirationDate());
        project.setEnabled(manageProjectDto.isEnabled());
        projectRepository.saveAndFlush(project);

        if (manageProjectDto.getUsersIds() != null) {
            List<User> users = userRepository.findAllByIdIn(manageProjectDto.getUsersIds());
            if (manageProjectDto.getUsersIds().size() > 0)
                for (User user : users) {
                    user.getProjects().add(project);
                    userRepository.save(user);
                }

            project.setUsers(new HashSet<>(users));
        }

        if (manageProjectDto.getTemplatesIds() != null) {
            List<Template> templates = templateRepository.findAllByIdIn(manageProjectDto.getTemplatesIds());
            if (manageProjectDto.getTemplatesIds().size() > 0)
                for (Template template : templates) {
                    template.getProjects().add(project);
                    templateRepository.save(template);
                }

            project.setTemplates(new HashSet<>(templates));
        }

        return projectRepository.save(project);
    }

    public List<Project> getProjects() {
        return projectRepository.findAllByDeletedOrderById(false);
    }

    public Project modifyProject(ManageProjectDto manageProjectDto) throws ItemNotFoundException, ModifyByNullEntityIdException {
        if (manageProjectDto.getId() == null)
            throw new ModifyByNullEntityIdException(Project.class);

        Project project = projectRepository.findByIdAndDeleted(manageProjectDto.getId(), false);

        if (project == null)
            throw new ItemNotFoundException(Project.class, manageProjectDto.getId());

        project.setName(manageProjectDto.getName());
        project.setDescription(manageProjectDto.getDescription());
        project.setEnabled(manageProjectDto.isEnabled());
        project.setExpirationDate(manageProjectDto.getExpirationDate());

        if (manageProjectDto.getUsersIds() != null) {
            if (!manageProjectDto.getUsersIds().equals(project.getUsersIds())) {
                for (User user : project.getUsers()) {
                    user.getProjects().remove(project);
                    userRepository.save(user);
                }
                List<User> users = userRepository.findAllByIdIn(manageProjectDto.getUsersIds());
                if (manageProjectDto.getUsersIds().size() > 0) {
                    for (User user : users) {
                        user.getProjects().add(project);
                        userRepository.save(user);
                    }
                }
                project.setUsers(new HashSet<>(users));
            }
        }

        if (manageProjectDto.getTemplatesIds() != null) {
            if (!manageProjectDto.getTemplatesIds().equals(project.getTemplatesIds())) {
                for (Template template : project.getTemplates()) {
                    template.getProjects().remove(project);
                    templateRepository.save(template);
                }
                List<Template> templates = templateRepository.findAllByIdIn(manageProjectDto.getTemplatesIds());
                if (manageProjectDto.getTemplatesIds().size() > 0) {
                    for (Template template : templates) {
                        template.getProjects().add(project);
                        templateRepository.save(template);
                    }
                }
                project.setTemplates(new HashSet<>(templates));
            }
        }

        return projectRepository.save(project);
    }

    public void deleteProject(int projectId) throws ItemNotFoundException {
        Project project = projectRepository.findByIdAndDeleted(projectId, false);

        if (project == null)
            throw new ItemNotFoundException(Project.class, projectId);

        for (User user : project.getUsers()) {
            user.getProjects().remove(project);
            userRepository.save(user);
        }

        // TODO Remove all artefacts and links between them
        // Teams, Classes, Enums, Groups, Buckets, Tags, Columns, Filters, Views, Tasks


        project.setDeleted(true);
        projectRepository.save(project);
    }

}
