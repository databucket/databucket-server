package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.ManageProjectDto;
import pl.databucket.server.entity.Project;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.ManageProjectService;

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
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
