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
import pl.databucket.server.exception.ProjectNotEmptyException;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.repository.ProjectRepository;
import pl.databucket.server.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class ManageProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ProjectContentService projectContentService;


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
        manageProjectDto.setEmpty(true);
        AuditLog.log("project.create", "id={} name={}", project.getId(), project.getName());
        return manageProjectDto;
    }

    @Transactional(readOnly = true)
    public List<ManageProjectDto> getProjects() {
        return projectRepository.findAllByDeletedOrderById(false).stream()
                .map(this::toDto)
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
        manageProjectDto.setEmpty(projectContentService.isEmpty(project.getId()));
        AuditLog.log("project.modify", "id={} name={}", project.getId(), project.getName());
        return manageProjectDto;
    }

    @Transactional
    public void deleteProject(int projectId) throws ItemNotFoundException, ProjectNotEmptyException {
        Project project = projectRepository.findByIdAndDeleted(projectId, false);

        if (project == null)
            throw new ItemNotFoundException(Project.class, projectId);

        // only an empty project: clearing it is a separate, confirmed step
        if (!projectContentService.isEmpty(projectId))
            throw new ProjectNotEmptyException(project.getName(), "deleting it");

        for (User user : project.getUsers()) {
            user.getProjects().remove(project);
            userRepository.save(user);
        }

        // what is left: objects deleted before (only marked as deleted) and their bucket tables
        projectContentService.clearProject(projectId);

        project.setDeleted(true);
        projectRepository.save(project);
        AuditLog.log("project.delete", "id={} name={}", project.getId(), project.getName());
    }

    private ManageProjectDto toDto(Project project) {
        ManageProjectDto dto = modelMapper.map(project, ManageProjectDto.class);
        dto.setEmpty(projectContentService.isEmpty(project.getId()));
        return dto;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getProjectContent(int projectId) throws ItemNotFoundException {
        if (projectRepository.findByIdAndDeleted(projectId, false) == null)
            throw new ItemNotFoundException(Project.class, projectId);
        return projectContentService.getContent(projectId);
    }

    // removes everything in the project except the project itself and its users
    @Transactional
    public void clearProject(int projectId) throws ItemNotFoundException {
        Project project = projectRepository.findByIdAndDeleted(projectId, false);
        if (project == null)
            throw new ItemNotFoundException(Project.class, projectId);
        projectContentService.clearProject(projectId);
        AuditLog.log("project.clear", "id={} name={}", project.getId(), project.getName());
    }

}
