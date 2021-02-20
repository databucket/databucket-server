package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.ManageProjectDto;
import pl.databucket.entity.Project;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.service.ManageProjectService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@PreAuthorize("hasRole('SUPER')")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/manage/projects")
@RestController
public class ManageProjectController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(ManageProjectController.class);

    @Autowired
    private ManageProjectService manageProjectService;

    @Autowired
    private ModelMapper modelMapper;


    @PostMapping
    public ResponseEntity<?> createProject(@Valid @RequestBody ManageProjectDto manageProjectDto) {
        try {
            Project project = manageProjectService.createProject(manageProjectDto);
            modelMapper.map(project, manageProjectDto);
            return new ResponseEntity<>(manageProjectDto, HttpStatus.CREATED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

//    @GetMapping
//    public ResponseEntity<?> getProjects(ProjectSpecification specification, Pageable pageable) {
//        try {
//            Page<Project> projectPage = projectService.getProjects(specification, pageable);
//            return new ResponseEntity<>(new ProjectPageResponse(projectPage, modelMapper), HttpStatus.OK);
//        } catch (Exception ee) {
//            return exceptionFormatter.defaultException(ee);
//        }
//    }

    @GetMapping
    public ResponseEntity<?> getProjects() {
        try {
            List<Project> projects = manageProjectService.getProjects();
            List<ManageProjectDto> projectsDto = projects.stream().map(item -> modelMapper.map(item, ManageProjectDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(projectsDto, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyProject(@Valid @RequestBody ManageProjectDto manageProjectDto) {
        try {
            Project project = manageProjectService.modifyProject(manageProjectDto);
            modelMapper.map(project, manageProjectDto);
            return new ResponseEntity<>(manageProjectDto, HttpStatus.OK);
        } catch (ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @DeleteMapping(value = "/{projectId}")
    public ResponseEntity<?> deleteProject(@PathVariable Integer projectId) {
        try {
            manageProjectService.deleteProject(projectId);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
