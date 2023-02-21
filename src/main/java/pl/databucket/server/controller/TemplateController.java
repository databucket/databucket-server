package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.dto.TemplateDto;
import pl.databucket.server.entity.Template;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.exception.WrongProjectException;
import pl.databucket.server.security.CustomUserDetails;
import pl.databucket.server.service.template.TemplateService;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/templates")
@RestController
@RequiredArgsConstructor
public class TemplateController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(TemplateController.class);

    private final TemplateService templateService;
    private final ModelMapper modelMapper;


    @PreAuthorize("hasRole('SUPER')")
    @PostMapping
    public ResponseEntity<TemplateDto> createTemplate(@Valid @RequestBody TemplateDto templateDto)
        throws ItemNotFoundException {
        Template template = templateService.createTemplate(templateDto);
        modelMapper.map(template, templateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(templateDto);
    }

    @PreAuthorize("hasRole('SUPER')")
    @GetMapping
    public ResponseEntity<List<TemplateDto>> getTemplates() {
        List<Template> templates = templateService.getTemplates();
        List<TemplateDto> templatesDto = templates.stream().map(item -> modelMapper.map(item, TemplateDto.class))
            .toList();
        return ResponseEntity.ok(templatesDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = {"/project/{projectId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TemplateDto>> getProjectTemplates(
        @Parameter(name = "id", example = "1", required = true) @PathVariable Integer projectId)
        throws ItemNotFoundException {
        // Check if user has token generated to the projectId
        if (!((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication()
            .getPrincipal()).getProjectId().equals(projectId)) {
            throw new WrongProjectException();
        }

        List<Template> templates = templateService.getTemplates(projectId);
        List<TemplateDto> templatesDto = templates.stream().map(item -> modelMapper.map(item, TemplateDto.class))
            .collect(Collectors.toList());
        return ResponseEntity.ok(templatesDto);
    }

    @PreAuthorize("hasRole('SUPER')")
    @PutMapping
    public ResponseEntity<TemplateDto> modifyTemplate(@Valid @RequestBody TemplateDto templateDto)
        throws ModifyByNullEntityIdException, ItemNotFoundException {
        Template template = templateService.modifyTemplate(templateDto);
        modelMapper.map(template, templateDto);
        return ResponseEntity.ok(templateDto);
    }

    @PreAuthorize("hasRole('SUPER')")
    @DeleteMapping(value = "/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable int templateId) throws ItemNotFoundException {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping(value = "/run/{templateId}")
    public ResponseEntity<Void> runTemplate(@PathVariable int templateId)
        throws ItemAlreadyExistsException, ItemNotFoundException, SomeItemsNotFoundException {
        templateService.runTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleError(Exception ex) {
        return exceptionFormatter.defaultException(ex);
    }

    @ExceptionHandler({ItemNotFoundException.class, ModifyByNullEntityIdException.class})
    public ResponseEntity<Map<String, Object>> handleNotFoundError(Exception ex) {
        return exceptionFormatter.customException(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WrongProjectException.class)
    public ResponseEntity<Map<String, Object>> handleError(WrongProjectException ex) {
        return exceptionFormatter.customException(ex, HttpStatus.NOT_ACCEPTABLE);
    }

}
