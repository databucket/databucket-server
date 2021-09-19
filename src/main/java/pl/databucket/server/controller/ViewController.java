package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.ViewDto;
import pl.databucket.server.entity.View;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.ViewService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/views")
@RestController
public class ViewController {

  private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(ViewController.class);

  @Autowired
  private ViewService viewService;

  @Autowired
  private ModelMapper modelMapper;

  @PostMapping
  public ResponseEntity<?> createView(@Valid @RequestBody ViewDto viewDto) {
    try {
      View view = viewService.createView(viewDto);
      modelMapper.map(view, viewDto);
      return new ResponseEntity<>(viewDto, HttpStatus.CREATED);
    } catch (ItemNotFoundException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

  @GetMapping
  public ResponseEntity<?> getViews() {
    try {
      List<View> views = viewService.getViews();
      List<ViewDto> viewsDto = views.stream().map(item -> modelMapper.map(item, ViewDto.class)).collect(Collectors.toList());
      return new ResponseEntity<>(viewsDto, HttpStatus.OK);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }

  @PutMapping
  public ResponseEntity<?> modifyView(@Valid @RequestBody ViewDto viewDto) {
    try {
      View view = viewService.modifyView(viewDto);
      modelMapper.map(view, viewDto);
      return new ResponseEntity<>(viewDto, HttpStatus.OK);
    } catch (ItemNotFoundException | ModifyByNullEntityIdException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

  @DeleteMapping(value = "/{viewId}")
  public ResponseEntity<?> deleteViews(@PathVariable long viewId) {
    try {
      viewService.deleteView(viewId);
      return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

}
