package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.dto.ManageProjectDto;
import pl.databucket.entity.Project;
import pl.databucket.entity.User;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.repository.ProjectRepository;
import pl.databucket.repository.UserRepository;

import java.util.HashSet;
import java.util.List;


@Service
public class ManageProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

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
