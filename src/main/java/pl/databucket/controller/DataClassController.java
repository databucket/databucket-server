package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.DataClassDto;
import pl.databucket.entity.DataClass;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.ItemAlreadyExistsException;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.response.DataClassPageResponse;
import pl.databucket.service.DataClassService;
import pl.databucket.specification.DataClassSpecification;

import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/classes")
@RestController
public class DataClassController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(DataClassController.class);

    @Autowired
    private DataClassService dataClassService;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<?> createDataClass(@Valid @RequestBody DataClassDto dataClassDto) {
        try {
            DataClass dataClass = dataClassService.createDataClass(dataClassDto);
            modelMapper.map(dataClass, dataClassDto);
            return new ResponseEntity<>(dataClassDto, HttpStatus.CREATED);
        } catch (ItemAlreadyExistsException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getDataClasses(DataClassSpecification specification, Pageable pageable) {
        try {
            Page<DataClass> dataClassPage = dataClassService.getDataClasses(specification, pageable);
            return new ResponseEntity<>(new DataClassPageResponse(dataClassPage, modelMapper), HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyDataClass(@Valid @RequestBody DataClassDto dataClassDto) {
        try {
            DataClass dataClass = dataClassService.modifyDataClass(dataClassDto);
            modelMapper.map(dataClass, dataClassDto);
            return new ResponseEntity<>(dataClassDto, HttpStatus.OK);
        } catch (ItemNotFoundException | ModifyByNullEntityIdException e) {
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
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
