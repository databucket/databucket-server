package pl.databucket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.ColumnsDto;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.ItemAlreadyUsedException;
import pl.databucket.service.ColumnsService;
import pl.databucket.specification.ColumnsSpecification;

import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/columns")
@RestController
public class ColumnsController {

    @Autowired
    private ColumnsService columnsService;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(ColumnsController.class);

    @PostMapping
    public ResponseEntity<?> createColumns(@Valid @RequestBody ColumnsDto columnsDto) {
        try {
            return new ResponseEntity<>(columnsService.createColumns(columnsDto), HttpStatus.CREATED);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @GetMapping
    public ResponseEntity<?> getColumns(ColumnsSpecification specification, Pageable pageable) {
        try {
            return new ResponseEntity<>(columnsService.getColumns(specification, pageable), HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyColumns(@Valid @RequestBody ColumnsDto columnsDto) {
        try {
            return new ResponseEntity<>(columnsService.modifyColumns(columnsDto), HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @DeleteMapping(value =  "/{columnsId}")
    public ResponseEntity<?> deleteColumns(@PathVariable long columnsId) {
        try {
            columnsService.deleteColumns(columnsId);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ItemAlreadyUsedException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }
}
