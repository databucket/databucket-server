package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.dto.ProjectDto;
import pl.databucket.entity.Project;
import pl.databucket.exception.ItemAlreadyExistsException;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.repository.ProjectRepository;

import java.util.List;


@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    public Project createProject(ProjectDto projectDto) {
        Project project = new Project();
        project.setName(projectDto.getName());
        project.setDescription(projectDto.getDescription());
        project.setExpirationDate(projectDto.getExpirationDate());
        project.setEnabled(projectDto.isEnabled());

        return projectRepository.save(project);
    }

    public Page<Project> getProjects(Specification<Project> specification, Pageable pageable) {
        return projectRepository.findAll(specification, pageable);
    }

    public List<Project> getProjects() {
        return projectRepository.findAll();
    }

    public Project modifyProject(ProjectDto projectDto) throws ItemNotFoundException, ModifyByNullEntityIdException {
        if (projectDto.getId() == null)
            throw new ModifyByNullEntityIdException(Project.class);

        Project project = projectRepository.findByIdAndDeleted(projectDto.getId(), false);

        if (project == null)
            throw new ItemNotFoundException(Project.class, projectDto.getId());

        project.setName(projectDto.getName());
        project.setDescription(projectDto.getDescription());
        project.setEnabled(projectDto.isEnabled());
        project.setExpirationDate(projectDto.getExpirationDate());
        return projectRepository.save(project);
    }

    public void deleteProject(int projectId) throws ItemNotFoundException {
        Project project = projectRepository.findByIdAndDeleted(projectId, false);

        if (project == null)
            throw new ItemNotFoundException(Project.class, projectId);

        project.setDeleted(true);
        projectRepository.save(project);
    }

}
