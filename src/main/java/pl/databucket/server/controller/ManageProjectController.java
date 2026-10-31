package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.ManageProjectDto;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.exception.ProjectNotEmptyException;
import pl.databucket.server.service.DatasetService;
import pl.databucket.server.service.ManageProjectService;

import jakarta.validation.Valid;
import java.util.Map;

@Hidden
@PreAuthorize("hasRole('SUPER')")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/manage/projects")
@RestController
public class ManageProjectController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(ManageProjectController.class);

    @Autowired
    private ManageProjectService manageProjectService;

    @Autowired
    private DatasetService datasetService;


    @PostMapping
    public ResponseEntity<?> createProject(@Valid @RequestBody ManageProjectDto manageProjectDto) {
        try {
            return new ResponseEntity<>(manageProjectService.createProject(manageProjectDto), HttpStatus.CREATED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getProjects() {
        try {
            return new ResponseEntity<>(manageProjectService.getProjects(), HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyProject(@Valid @RequestBody ManageProjectDto manageProjectDto) {
        try {
            return new ResponseEntity<>(manageProjectService.modifyProject(manageProjectDto), HttpStatus.OK);
        } catch (ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    // what clearing (or deleting) the project removes
    @GetMapping(value = "/{projectId}/content")
    public ResponseEntity<?> getProjectContent(@PathVariable Integer projectId) {
        try {
            return new ResponseEntity<>(manageProjectService.getProjectContent(projectId), HttpStatus.OK);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    // the datasets that can be imported into an empty project (e.g. DEMO)
    @GetMapping(value = "/datasets")
    public ResponseEntity<?> getDatasets() {
        try {
            return new ResponseEntity<>(datasetService.getDatasets(), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PostMapping(value = "/{projectId}/datasets/{datasetId}")
    public ResponseEntity<?> importDataset(@PathVariable Integer projectId, @PathVariable String datasetId) {
        try {
            return new ResponseEntity<>(Map.of("rows", datasetService.importDataset(projectId, datasetId)), HttpStatus.OK);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (ProjectNotEmptyException e) {
            return exceptionFormatter.customException(e, HttpStatus.CONFLICT);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    // removes everything in the project except the project itself and its users
    @PostMapping(value = "/{projectId}/clear")
    public ResponseEntity<?> clearProject(@PathVariable Integer projectId) {
        try {
            manageProjectService.clearProject(projectId);
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (ItemNotFoundException e) {
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
        } catch (ProjectNotEmptyException e) {
            return exceptionFormatter.customException(e, HttpStatus.CONFLICT);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
