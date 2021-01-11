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


@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    public Project createProject(ProjectDto projectDto) throws ItemAlreadyExistsException {
        if (projectRepository.existsByNameAndDeleted(projectDto.getName(), false))
            throw new ItemAlreadyExistsException(Project.class, projectDto.getName());

        Project project = new Project();
        project.setName(projectDto.getName());
        project.setDescription(projectDto.getDescription());

        return projectRepository.save(project);
    }

    public Page<Project> getProjects(Specification<Project> specification, Pageable pageable) {
        return projectRepository.findAll(specification, pageable);
    }

    public Project modifyProject(ProjectDto projectDto) throws ItemNotFoundException, ItemAlreadyExistsException, ModifyByNullEntityIdException {
        if (projectDto.getId() == null)
            throw new ModifyByNullEntityIdException(Project.class);

        Project project = projectRepository.findByIdAndDeleted(projectDto.getId(), false);

        if (project == null)
            throw new ItemNotFoundException(Project.class, projectDto.getId());

        if (!project.getName().equals(projectDto.getName()))
            if (projectRepository.existsByNameAndDeleted(projectDto.getName(), false))
                throw new ItemAlreadyExistsException(Project.class, projectDto.getName());

        project.setName(projectDto.getName());
        project.setDescription(projectDto.getDescription());
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
