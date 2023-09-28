package pl.databucket.server.controller;

import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.security.TokenProvider;
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
    public ResponseEntity<?> createTemplate(@Valid @RequestBody TemplateDto templateDto) {
        try {
            Template template = templateService.createTemplate(templateDto);
            modelMapper.map(template, templateDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(templateDto);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasRole('SUPER')")
    @GetMapping
    public ResponseEntity<?> getTemplates() {
        try {
            List<Template> templates = templateService.getTemplates();
            List<TemplateDto> templatesDto = templates.stream().map(item -> modelMapper.map(item, TemplateDto.class))
                .toList();
            return ResponseEntity.ok(templatesDto);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = {"/project/{projectId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProjectTemplates(
        @ApiParam(value = "id", example = "1", required = true) @PathVariable Integer projectId,
        Authentication auth) {
        try {
            // Check if user has token generated to the projectId
            Jwt jwt = (Jwt) auth.getPrincipal();
            Long currentProjectId = jwt.getClaim(TokenProvider.PROJECT_ID);

            if (!projectId.equals(currentProjectId.intValue())) {
                return exceptionFormatter.customException("Incorrect project", HttpStatus.NOT_ACCEPTABLE);
            }

            List<Template> templates = templateService.getTemplates(projectId);
            List<TemplateDto> templatesDto = templates.stream().map(item -> modelMapper.map(item, TemplateDto.class))
                .toList();
            return ResponseEntity.ok(templatesDto);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasRole('SUPER')")
    @PutMapping
    public ResponseEntity<?> modifyTemplate(@Valid @RequestBody TemplateDto templateDto) {
        try {
            Template template = templateService.modifyTemplate(templateDto);
            modelMapper.map(template, templateDto);
            return ResponseEntity.ok(templateDto);
        } catch (ItemNotFoundException | ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasRole('SUPER')")
    @DeleteMapping(value = "/{templateId}")
    public ResponseEntity<?> deleteTemplate(@PathVariable int templateId) {
        try {
            templateService.deleteTemplate(templateId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping(value = "/run/{templateId}")
    public ResponseEntity<?> runTemplate(@PathVariable int templateId) {
        try {
            templateService.runTemplate(templateId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

}
