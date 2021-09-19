package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.DataFilterDto;
import pl.databucket.server.entity.DataFilter;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.DataFilterService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/filters")
@RestController
public class DataFilterController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(DataFilterController.class);

    @Autowired
    private DataFilterService filterService;

    @Autowired
    private ModelMapper modelMapper;

    @PreAuthorize("hasRole('ADMIN')")
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
    public ResponseEntity<?> getFilters() {
        try {
            List<DataFilter> dataFilters = filterService.getFilters();
            List<DataFilterDto> dataFiltersDto = dataFilters.stream().map(item -> modelMapper.map(item, DataFilterDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(dataFiltersDto, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{filterId}")
    public ResponseEntity<?> deleteFilters(@PathVariable long filterId) {
        try {
            filterService.deleteFilter(filterId);
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
