package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.DataClassDto;
import pl.databucket.server.entity.DataClass;
import pl.databucket.server.exception.*;
import pl.databucket.server.service.DataClassService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/classes")
@RestController
public class DataClassController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(DataClassController.class);

    @Autowired
    private DataClassService dataClassService;

    @Autowired
    private ModelMapper modelMapper;

    @PreAuthorize("hasRole('ADMIN')")
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
    public ResponseEntity<?> getDataClasses() {
        try {
            List<DataClass> dataClasses = dataClassService.getDataClasses();
            List<DataClassDto> dataClassesDto = dataClasses.stream().map(item -> modelMapper.map(item, DataClassDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(dataClassesDto, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{classId}")
    public ResponseEntity<?> deleteDataClass(@PathVariable("classId") long classId) {
        try {
            dataClassService.deleteDataClass(classId);
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (ItemNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (ItemAlreadyUsedException e) {
            return exceptionFormatter.customException(e, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
