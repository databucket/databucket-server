package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.GroupDto;
import pl.databucket.entity.Group;
import pl.databucket.exception.*;
import pl.databucket.response.GroupPageResponse;
import pl.databucket.service.GroupService;
import pl.databucket.specification.GroupSpecification;

import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/groups")
@RestController
public class GroupController {

  @Autowired
  private GroupService groupService;

  @Autowired
  private ModelMapper modelMapper;

  private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(GroupController.class);

  @PostMapping
  public ResponseEntity<?> createGroup(@Valid @RequestBody GroupDto groupDto) {
    try {
      Group group = groupService.createGroup(groupDto);
      modelMapper.map(group, groupDto);
      return new ResponseEntity<>(groupDto, HttpStatus.CREATED);
    } catch (ItemAlreadyExistsException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
    } catch (SomeItemsNotFoundException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

  @GetMapping
  public ResponseEntity<?> getGroups(GroupSpecification customerSpec, Pageable pageable) {
    try {
      return new ResponseEntity<>(new GroupPageResponse(groupService.getGroups(customerSpec, pageable), modelMapper), HttpStatus.OK);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }

  @PutMapping
  public ResponseEntity<?> modifyGroup(@Valid @RequestBody GroupDto groupDto) {
    try {
      Group group = groupService.modifyGroup(groupDto);
      modelMapper.map(group, groupDto);
      return new ResponseEntity<>(groupDto, HttpStatus.OK);
    } catch (ItemNotFoundException | SomeItemsNotFoundException | ModifyByNullEntityIdException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }

  @DeleteMapping(value = "/{groupId}")
  public ResponseEntity<?> deleteGroup(@PathVariable("groupId") Long groupId) {
    try {
      groupService.deleteGroup(groupId);
      return new ResponseEntity<>(null, HttpStatus.OK);
    } catch (ItemNotFoundException e) {
      return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }
}
