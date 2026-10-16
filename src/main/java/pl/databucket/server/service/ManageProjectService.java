package pl.databucket.server.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.databucket.server.dto.ManageProjectDto;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.ProjectRepository;
import pl.databucket.server.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class ManageProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;


    @Transactional
    public ManageProjectDto createProject(ManageProjectDto manageProjectDto) {
        Project project = new Project();
        project.setName(manageProjectDto.getName());
        project.setDescription(manageProjectDto.getDescription());
        project.setExpirationDate(manageProjectDto.getExpirationDate());
        project.setEnabled(manageProjectDto.isEnabled());
        project.setPublicVisible(manageProjectDto.isPublicVisible());
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

        modelMapper.map(projectRepository.save(project), manageProjectDto);
        return manageProjectDto;
    }

    @Transactional(readOnly = true)
    public List<ManageProjectDto> getProjects() {
        return projectRepository.findAllByDeletedOrderById(false).stream()
                .map(project -> modelMapper.map(project, ManageProjectDto.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public ManageProjectDto modifyProject(ManageProjectDto manageProjectDto) throws ItemNotFoundException, ModifyByNullEntityIdException {
        if (manageProjectDto.getId() == null)
            throw new ModifyByNullEntityIdException(Project.class);

        Project project = projectRepository.findByIdAndDeleted(manageProjectDto.getId(), false);

        if (project == null)
            throw new ItemNotFoundException(Project.class, manageProjectDto.getId());

        project.setName(manageProjectDto.getName());
        project.setDescription(manageProjectDto.getDescription());
        project.setEnabled(manageProjectDto.isEnabled());
        project.setPublicVisible(manageProjectDto.isPublicVisible());
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

        modelMapper.map(projectRepository.save(project), manageProjectDto);
        return manageProjectDto;
    }

    @Transactional
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
