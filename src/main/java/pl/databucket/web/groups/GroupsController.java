package pl.databucket.web.groups;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.exception.EmptyInputValueException;
import pl.databucket.exception.ExceededMaximumNumberOfCharactersException;
import pl.databucket.exception.GroupAlreadyExistsException;
import pl.databucket.exception.IncorrectValueException;
import pl.databucket.exception.ItemDoNotExistsException;
import pl.databucket.database.C;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.service.DatabucketService;
import pl.databucket.service.ResponseBody;
import pl.databucket.service.ResponseStatus;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/groups")
@RestController
public class GroupsController {

  private static final Logger logger = LoggerFactory.getLogger(GroupsController.class);

  private final DatabucketService databucketService;

  @Autowired
  public GroupsController(DatabucketService databucketService) {
    this.databucketService = databucketService;
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> createGroup(@RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
    ResponseBody rb = new ResponseBody();

    String groupName;
    String description;
    ArrayList<Integer> buckets;

    try {
      groupName = FieldValidator.validateGroupName(body, true);
      description = FieldValidator.validateDescription(body, false);
      buckets = FieldValidator.validateBuckets(body, false);

      int groupId = databucketService.createGroup(userName, groupName, description, buckets);
      rb.setStatus(ResponseStatus.OK);
      rb.setGroupId(groupId);
      rb.setMessage("The group '" + groupName + "' has been successfully created.");
      return new ResponseEntity<>(rb, HttpStatus.CREATED);
    } catch (GroupAlreadyExistsException | ExceededMaximumNumberOfCharactersException | EmptyInputValueException e) {
      return customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @DeleteMapping(value = "/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> deleteGroup(@PathVariable("groupId") Integer groupId, @RequestParam String userName) {
    ResponseBody rb = new ResponseBody();
    try {
      databucketService.deleteGroup(groupId, userName);
      rb.setStatus(ResponseStatus.OK);
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @SuppressWarnings("unchecked")
  @GetMapping(value = {"", "/{groupId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> getGroups(
      @PathVariable Optional<Integer> groupId,
      @RequestParam(required = false) Optional<Integer> page,
      @RequestParam(required = false) Optional<Integer> limit,
      @RequestParam(required = false) Optional<String> sort,
      @RequestParam(required = false) Optional<String> filter) {
    ResponseBody rb = new ResponseBody();

    try {
      if (page.isPresent()) {
        FieldValidator.mustBeGraterThen0("page", page.get());
        rb.setPage(page.get());
      }

      if (limit.isPresent()) {
        FieldValidator.mustBeGraterOrEqual0("limit", limit.get());
        rb.setLimit(limit.get());
      }

      if (sort.isPresent()) {
        FieldValidator.validateSort(sort.get());
        rb.setSort(sort.get());
      }

      List<Condition> urlConditions = null;
      if (filter.isPresent()) {
        urlConditions = FieldValidator.validateFilter(filter.get());
      }

      Map<String, Object> result = databucketService.getGroups(groupId, page, limit, sort, urlConditions);

      long total = (long) result.get(C.TOTAL);
      rb.setTotal(total);

      if (page.isPresent() && limit.isPresent()) {
        rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
      }

      rb.setGroups((List<Map<String, Object>>) result.get(C.GROUPS));

      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (IncorrectValueException e) {
      return customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @PutMapping(value = "/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> modifyGroup(@PathVariable("groupId") Integer groupId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {

    ResponseBody rb = new ResponseBody();

    try {
      databucketService.modifyGroup(userName, groupId, body);
      rb.setStatus(ResponseStatus.OK);
      rb.setMessage("Group '" + groupId + "' has been successfully modified.");
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e1) {
      return customException(rb, e1, HttpStatus.NOT_FOUND);
    } catch (GroupAlreadyExistsException | ExceededMaximumNumberOfCharactersException | IncorrectValueException e2) {
      return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  private ResponseEntity<ResponseBody> defaultException(ResponseBody rb, Exception e) {
    logger.error("ERROR:", e);
    rb.setStatus(ResponseStatus.FAILED);
    rb.setMessage(e.getMessage());
    return new ResponseEntity<>(rb, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ResponseBody> customException(ResponseBody rb, Exception e, HttpStatus status) {
    logger.warn(e.getMessage());
    rb.setStatus(ResponseStatus.FAILED);
    rb.setMessage(e.getMessage());
    return new ResponseEntity<>(rb, status);
  }
}
