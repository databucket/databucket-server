package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.DataColumnsDto;
import pl.databucket.server.entity.DataColumns;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.DataColumnsService;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@PreAuthorize("hasRole('ADMIN')")
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
    public ResponseEntity<?> getColumns() {
        try {
            List<DataColumns> dataColumns = columnsService.getColumns();
            List<DataColumnsDto> dataColumnsDto = dataColumns.stream().map(item -> modelMapper.map(item, DataColumnsDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(dataColumnsDto, HttpStatus.OK);
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
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (EntityNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }
}
