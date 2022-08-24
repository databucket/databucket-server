package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.TemplateDataItemDto;
import pl.databucket.server.entity.TemplateDataItem;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.template.TemplateDataItemsService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/templates/data")
@RestController
public class TemplateDataItemsController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(TemplateDataItemsController.class);

    @Autowired
    private TemplateDataItemsService templateDataItemsService;

    @Autowired
    private ModelMapper modelMapper;


    @PreAuthorize("hasRole('SUPER')")
    @PostMapping(value = "/items")
    public ResponseEntity<?> createTemplateDataItem(@Valid @RequestBody TemplateDataItemDto templateDataItemDto) {
        try {
            TemplateDataItem templateDataItem = templateDataItemsService.createTemplateDataItem(templateDataItemDto);
            modelMapper.map(templateDataItem, templateDataItemDto);
            return new ResponseEntity<>(templateDataItemDto, HttpStatus.CREATED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasRole('SUPER')")
    @GetMapping(value = {"/{dataId}/items"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getTemplateDataItems(@PathVariable Integer dataId) {
        try {
            List<TemplateDataItem> templateDataItems = templateDataItemsService.getTemplateDataItems(dataId);
            List<TemplateDataItemDto> templateDataItemList = templateDataItems.stream().map(item -> modelMapper.map(item, TemplateDataItemDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(templateDataItemList, HttpStatus.OK);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasRole('SUPER')")
    @PutMapping(value = "/items")
    public ResponseEntity<?> modifyTemplateDataItem(@Valid @RequestBody TemplateDataItemDto templateDataItemDto) {
        try {
            TemplateDataItem templateDataItem = templateDataItemsService.modifyTemplateDataItem(templateDataItemDto);
            modelMapper.map(templateDataItem, templateDataItemDto);
            return new ResponseEntity<>(templateDataItemDto, HttpStatus.OK);
        } catch (ItemNotFoundException | ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasRole('SUPER')")
    @DeleteMapping(value = "/items/{templateDataItemId}")
    public ResponseEntity<?> deleteTemplateDataItem(@PathVariable long templateDataItemId) {
        try {
            templateDataItemsService.deleteTemplateDataItem(templateDataItemId);
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
