package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.TemplateDto;
import pl.databucket.server.entity.Template;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.template.TemplateService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/template")
@RestController
public class TemplateController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(TemplateController.class);

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ModelMapper modelMapper;

    @PreAuthorize("hasRole('SUPER')")
    @PostMapping
    public ResponseEntity<?> createTemplate(@Valid @RequestBody TemplateDto templateDto) {
        try {
            Template template = templateService.createTemplate(templateDto);
            modelMapper.map(template, templateDto);
            return new ResponseEntity<>(templateDto, HttpStatus.CREATED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<?> getTemplates() {
        try {
            List<Template> templates = templateService.getTemplates();
            List<TemplateDto> templatesDto = templates.stream().map(item -> modelMapper.map(item, TemplateDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(templatesDto, HttpStatus.OK);
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
            return new ResponseEntity<>(templateDto, HttpStatus.OK);
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
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasAnyRole('SUPER', 'ADMIN')")
    @PostMapping(value = "/run/{templateId}")
    public ResponseEntity<?> runTemplate(@PathVariable int templateId) {
        try {
            templateService.runTemplate(templateId);
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

}
