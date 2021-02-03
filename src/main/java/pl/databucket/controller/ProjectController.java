package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.ProjectDto;
import pl.databucket.entity.Project;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.response.ProjectPageResponse;
import pl.databucket.service.ProjectService;
import pl.databucket.specification.ProjectSpecification;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@PreAuthorize("hasRole('SUPER')")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/projects")
@RestController
public class ProjectController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(ProjectController.class);

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ModelMapper modelMapper;


    @PostMapping
    public ResponseEntity<?> createProject(@Valid @RequestBody ProjectDto projectDto) {
        try {
            Project project = projectService.createProject(projectDto);
            modelMapper.map(project, projectDto);
            return new ResponseEntity<>(projectDto, HttpStatus.CREATED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getProjects(ProjectSpecification specification, Pageable pageable) {
        try {
            Page<Project> projectPage = projectService.getProjects(specification, pageable);
            return new ResponseEntity<>(new ProjectPageResponse(projectPage, modelMapper), HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @GetMapping(value="/all")
    public ResponseEntity<?> getProjects() {
        try {
            List<Project> projects = projectService.getProjects();
            List<ProjectDto> projectsDto = projects.stream().map(item -> modelMapper.map(item, ProjectDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(projectsDto, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyProject(@Valid @RequestBody ProjectDto projectDto) {
        try {
            Project project = projectService.modifyProject(projectDto);
            modelMapper.map(project, projectDto);
            return new ResponseEntity<>(projectDto, HttpStatus.OK);
        } catch (ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @DeleteMapping(value = "/{projectId}")
    public ResponseEntity<?> deleteProject(@PathVariable Integer projectId) {
        try {
            projectService.deleteProject(projectId);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
