package pl.databucket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.DataClassDto;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.service.DataClassService;
import pl.databucket.specification.DataClassSpecification;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/classes")
@RestController
public class DataClassController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(DataClassController.class);

    @Autowired
    private DataClassService dataClassService;

    @PostMapping
    public ResponseEntity<?> createDataClass(@Valid @RequestBody DataClassDto dataClassDto) {
        try {
            return new ResponseEntity<>(dataClassService.createDataClass(dataClassDto), HttpStatus.CREATED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getDataClasses(DataClassSpecification specification, Pageable pageable) {
        try {
            return new ResponseEntity<>(dataClassService.getDataClasses(specification, pageable), HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyDataClass(@Valid @RequestBody DataClassDto dataClassDto) {
        try {
            return new ResponseEntity<>(dataClassService.modifyDataClass(dataClassDto), HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @DeleteMapping(value = "/{classId}")
    public ResponseEntity<?> deleteDataClass(@PathVariable("classId") long classId) {
        try {
            dataClassService.deleteDataClass(classId);
            return new ResponseEntity<>(null, HttpStatus.OK);

        } catch (EntityNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
