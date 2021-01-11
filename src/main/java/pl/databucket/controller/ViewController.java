package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.ViewDto;
import pl.databucket.entity.View;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.response.ViewPageResponse;
import pl.databucket.service.ViewService;
import pl.databucket.specification.ViewSpecification;
import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
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
  public ResponseEntity<?> getViews(ViewSpecification specification, Pageable pageable) {
    try {
      Page<View> viewPage = viewService.getViews(specification, pageable);
      return new ResponseEntity<>(new ViewPageResponse(viewPage, modelMapper), HttpStatus.OK);
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
      return new ResponseEntity<>(null, HttpStatus.OK);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

}
