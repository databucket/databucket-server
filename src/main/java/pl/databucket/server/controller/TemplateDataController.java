package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.TemplateDataDto;
import pl.databucket.server.entity.TemplateData;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.TemplateDataRepository;
import pl.databucket.server.service.template.TemplateDataItemsService;
import pl.databucket.server.service.template.TemplateDataService;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/templates")
@RestController
public class TemplateDataController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(TemplateDataController.class);

    @Autowired
    private TemplateDataRepository templateDataRepository;

    @Autowired
    private TemplateDataService templateDataService;

    @Autowired
    private TemplateDataItemsService templateDataItemsService;

    @Autowired
    private ModelMapper modelMapper;


    @PreAuthorize("hasRole('SUPER')")
    @PostMapping(value = "/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createTemplateData(@Valid @RequestBody TemplateDataDto templateDataDto) {
        try {
            TemplateData templateData = templateDataService.createTemplateData(templateDataDto);
            modelMapper.map(templateData, templateDataDto);
            return new ResponseEntity<>(templateDataDto, HttpStatus.CREATED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasRole('SUPER')")
    @GetMapping(value = "/{templateId}/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getTemplateData(@PathVariable int templateId) {
        try {
            List<TemplateData> templateData = templateDataService.getTemplateData(templateId);
            List<TemplateDataDto> templateDataList = templateData.stream().map(item -> modelMapper.map(item, TemplateDataDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(templateDataList, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasRole('SUPER')")
    @PutMapping(value = "/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> modifyTemplateData(@Valid @RequestBody TemplateDataDto templateDataDto) {
        try {
            TemplateData templateData = templateDataService.modifyTemplateData(templateDataDto);
            modelMapper.map(templateData, templateDataDto);
            return new ResponseEntity<>(templateDataDto, HttpStatus.OK);
        } catch (ItemNotFoundException | ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasRole('SUPER')")
    @DeleteMapping(value = "/data/{templateDataId}")
    @Transactional
    public ResponseEntity<?> deleteTemplateData(@PathVariable int templateDataId) {
        try {
            Optional<TemplateData> templateDataOpt = templateDataRepository.findById(templateDataId);

            if (!templateDataOpt.isPresent())
                throw new ItemNotFoundException(TemplateData.class, templateDataId);

            templateDataItemsService.deleteTemplateDataItems(templateDataOpt.get());
            templateDataService.deleteTemplateData(templateDataOpt.get());
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
