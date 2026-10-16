package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.*;
import pl.databucket.server.entity.*;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.service.*;

import java.util.List;
import java.util.stream.Collectors;

@Hidden
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/session")
@RestController
public class SessionController {

    @Autowired
    private AccessTreeService accessTreeService;

    @Autowired
    private DataColumnsService columnsService;

    @Autowired
    private DataFilterService filterService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ModelMapper modelMapper;


    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(SessionController.class);


    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @GetMapping(value="/access-tree")
    public ResponseEntity<?> getAccessTree() {
        try {
            AccessTreeDto accessTreeDto = accessTreeService.getAccessTree(SecurityContextHolder.getContext().getAuthentication().getName());
            return new ResponseEntity<>(accessTreeDto, HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @GetMapping(value="/columns/{ids}")
    public ResponseEntity<?> getUserColumns(@PathVariable List<Long> ids) {
        try {
            List<DataColumns> dataColumns = columnsService.getColumns(ids);
            List<UserColumnsDto> dataColumnsDto = dataColumns.stream().map(item -> modelMapper.map(item, UserColumnsDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(dataColumnsDto, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @GetMapping(value="/filters/{ids}")
    public ResponseEntity<?> getUserFilters(@PathVariable List<Long> ids) {
        try {
            List<DataFilter> dataFilters = filterService.getFilters(ids);
            List<DataFilterDto> dataFiltersDto = dataFilters.stream().map(item -> modelMapper.map(item, DataFilterDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(dataFiltersDto, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @GetMapping(value="/tasks/{ids}")
    public ResponseEntity<?> getUserTasks(@PathVariable List<Long> ids) {
        try {
            List<TaskDto> tasksDto = taskService.getTasks(ids);
            return new ResponseEntity<>(tasksDto, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

}
