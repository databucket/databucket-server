package pl.databucket.server.controller;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.dto.DataFilterDto;
import pl.databucket.server.entity.DataFilter;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemAlreadyUsedException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.DataFilterService;

@RequestMapping("/api/filters")
@RestController
@RequiredArgsConstructor
public class DataFilterController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(DataFilterController.class);

    private final DataFilterService filterService;
    private final ModelMapper modelMapper;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataFilterDto> createFilter(@Valid @RequestBody DataFilterDto dataFilterDto)
        throws ItemNotFoundException {
        DataFilter dataFilter = filterService.createFilter(dataFilterDto);
        modelMapper.map(dataFilter, dataFilterDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dataFilterDto);
    }

    @GetMapping
    public ResponseEntity<List<DataFilterDto>> getFilters() {
        List<DataFilter> dataFilters = filterService.getFilters();
        List<DataFilterDto> dataFiltersDto = dataFilters.stream()
            .map(item -> modelMapper.map(item, DataFilterDto.class)).toList();
        return ResponseEntity.ok(dataFiltersDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<DataFilterDto> modifyFilter(@Valid @RequestBody DataFilterDto dataFilterDto)
        throws ModifyByNullEntityIdException, ItemNotFoundException {
        DataFilter dataFilter = filterService.modifyFilter(dataFilterDto);
        modelMapper.map(dataFilter, dataFilterDto);
        return ResponseEntity.ok(dataFilterDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{filterId}")
    public ResponseEntity<Void> deleteFilters(@PathVariable long filterId)
        throws ItemAlreadyUsedException, ItemNotFoundException {
        filterService.deleteFilter(filterId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleError(Exception ex) {
        return exceptionFormatter.defaultException(ex);
    }
}
