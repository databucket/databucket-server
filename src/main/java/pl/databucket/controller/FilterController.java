package pl.databucket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.FilterDto;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.service.FilterService;
import pl.databucket.specification.FilterSpecification;

import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/filter")
@RestController
public class FilterController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(FilterController.class);

    @Autowired
    private FilterService filterService;


    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createFilter(@Valid @RequestBody FilterDto filterDto) {
        try {
            return new ResponseEntity<>(filterService.createFilter(filterDto), HttpStatus.CREATED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getFilters(FilterSpecification specification, Pageable pageable) {
        try {
            return new ResponseEntity<>(filterService.getFilters(specification, pageable), HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyFilter(@Valid @RequestBody FilterDto filterDto) {
        try {
            return new ResponseEntity<>(filterService.modifyFilter(filterDto), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @DeleteMapping(value = "/{filterId}")
    public ResponseEntity<?> deleteFilters(@PathVariable long filterId) {
        try {
            filterService.deleteFilter(filterId);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
