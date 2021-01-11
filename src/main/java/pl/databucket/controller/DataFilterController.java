package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.DataFilterDto;
import pl.databucket.entity.DataFilter;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.response.DataFilterPageResponse;
import pl.databucket.service.DataFilterService;
import pl.databucket.specification.FilterSpecification;

import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/filters")
@RestController
public class DataFilterController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(DataFilterController.class);

    @Autowired
    private DataFilterService filterService;

    @Autowired
    private ModelMapper modelMapper;


    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createFilter(@Valid @RequestBody DataFilterDto dataFilterDto) {
        try {
            DataFilter dataFilter = filterService.createFilter(dataFilterDto);
            modelMapper.map(dataFilter, dataFilterDto);
            return new ResponseEntity<>(dataFilterDto, HttpStatus.CREATED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getFilters(FilterSpecification specification, Pageable pageable) {
        try {
            Page<DataFilter> dataFilterPage = filterService.getFilters(specification, pageable);
            return new ResponseEntity<>(new DataFilterPageResponse(dataFilterPage, modelMapper), HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyFilter(@Valid @RequestBody DataFilterDto dataFilterDto) {
        try {
            DataFilter dataFilter = filterService.modifyFilter(dataFilterDto);
            modelMapper.map(dataFilter, dataFilterDto);
            return new ResponseEntity<>(dataFilterDto, HttpStatus.OK);
        } catch (ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
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
