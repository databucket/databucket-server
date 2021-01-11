package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.DataColumnsDto;
import pl.databucket.entity.DataColumns;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.response.DataColumnsPageResponse;
import pl.databucket.service.DataColumnsService;
import pl.databucket.specification.ColumnsSpecification;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/columns")
@RestController
public class DataColumnsController {

    @Autowired
    private DataColumnsService columnsService;

    @Autowired
    private ModelMapper modelMapper;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(DataColumnsController.class);

    @PostMapping
    public ResponseEntity<?> createColumns(@Valid @RequestBody DataColumnsDto columnsDto) {
        try {
            DataColumns dataColumns = columnsService.createColumns(columnsDto);
            modelMapper.map(dataColumns, columnsDto);
            return new ResponseEntity<>(columnsDto, HttpStatus.CREATED);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @GetMapping
    public ResponseEntity<?> getColumns(ColumnsSpecification specification, Pageable pageable) {
        try {
            Page<DataColumns> dataColumnsPage = columnsService.getColumns(specification, pageable);
            return new ResponseEntity<>(new DataColumnsPageResponse(dataColumnsPage, modelMapper), HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyColumns(@Valid @RequestBody DataColumnsDto dataColumnsDto) {
        try {
            DataColumns dataColumns = columnsService.modifyColumns(dataColumnsDto);
            modelMapper.map(dataColumns, dataColumnsDto);
            return new ResponseEntity<>(dataColumnsDto, HttpStatus.OK);
        } catch (EntityNotFoundException | ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @DeleteMapping(value =  "/{columnsId}")
    public ResponseEntity<?> deleteColumns(@PathVariable long columnsId) {
        try {
            columnsService.deleteColumns(columnsId);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }
}
