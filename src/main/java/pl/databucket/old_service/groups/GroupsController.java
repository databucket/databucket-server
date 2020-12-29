package pl.databucket.old_service.groups;

//@CrossOrigin(origins = "*", allowedHeaders = "*")
//@RequestMapping("/api/group-old")
//@RestController
public class GroupsController {

//  private final CustomExceptionFormatter customExceptionFormatter;
//  private final GroupsService service;
//
//  @Autowired
//  public GroupsController(GroupsService service) {
//    this.service = service;
//    this.customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(GroupsController.class));
//  }

//  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
//  public ResponseEntity<BaseResponse> createGroup(@RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
//    GroupsResponse rb = new GroupsResponse();
//
//    String groupName;
//    String description;
//    ArrayList<Integer> buckets;
//
//    try {
//      groupName = FieldValidator.validateGroupName(body, true);
//      description = FieldValidator.validateDescription(body, false);
//      buckets = FieldValidator.validateBuckets(body, false);
//
//      int groupId = service.createGroup(userName, groupName, description, buckets);
//      rb.setGroupId(groupId);
//      rb.setMessage("The group '" + groupName + "' has been successfully created.");
//      return new ResponseEntity<>(rb, HttpStatus.CREATED);
//    } catch (GroupAlreadyExistsException | ExceededMaximumNumberOfCharactersException | EmptyInputValueException e) {
//      return customExceptionFormatter.customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
//    } catch (Exception ee) {
//      return customExceptionFormatter.defaultException(rb, ee);
//    }
//  }
//
//  @DeleteMapping(value = "/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
//  public ResponseEntity<BaseResponse> deleteGroup(@PathVariable("groupId") Integer groupId, @RequestParam String userName) {
//    GroupsResponse rb = new GroupsResponse();
//    try {
//      service.deleteGroup(groupId, userName);
//      return new ResponseEntity<>(rb, HttpStatus.OK);
////    } catch (ItemDoNotExistsException e) {
////      return customExceptionFormatter.customException(rb, e, HttpStatus.NOT_FOUND);
//    } catch (Exception ee) {
//      return customExceptionFormatter.defaultException(rb, ee);
//    }
//  }
//
//  @GetMapping(value = {"", "/{groupId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
//  public ResponseEntity<BaseResponse> getGroups(
//      @PathVariable Optional<Integer> groupId,
//      @RequestParam(required = false) Optional<Integer> page,
//      @RequestParam(required = false) Optional<Integer> limit,
//      @RequestParam(required = false) Optional<String> sort,
//      @RequestParam(required = false) Optional<String> filter) {
//    GroupsResponse rb = new GroupsResponse();
//
//    try {
//      if (page.isPresent()) {
//        FieldValidator.mustBeGraterThen0("page", page.get());
//        rb.setPage(page.get());
//      }
//
//      if (limit.isPresent()) {
//        FieldValidator.mustBeGraterOrEqual0("limit", limit.get());
//        rb.setLimit(limit.get());
//      }
//
//      if (sort.isPresent()) {
//        FieldValidator.validateSort(sort.get());
//        rb.setSort(sort.get());
//      }
//
//      List<Condition> urlConditions = null;
//      if (filter.isPresent()) {
//        urlConditions = FieldValidator.validateFilter(filter.get());
//      }
//
//      Map<ResultField, Object> result = service.getGroups(groupId, page, limit, sort, urlConditions);
//
//      long total = (long) result.get(ResultField.TOTAL);
//      rb.setTotal(total);
//
//      if (page.isPresent() && limit.isPresent()) {
//        rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
//      }
//
//      rb.setGroups((List<Map<String, Object>>) result.get(ResultField.DATA));
//
//      return new ResponseEntity<>(rb, HttpStatus.OK);
//    } catch (IncorrectValueException e) {
//      return customExceptionFormatter.customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
//    } catch (Exception ee) {
//      return customExceptionFormatter.defaultException(rb, ee);
//    }
//  }
//
//  @PutMapping(value = "/{groupId}", produces = MediaType.APPLICATION_JSON_VALUE)
//  public ResponseEntity<BaseResponse> modifyGroup(@PathVariable("groupId") Integer groupId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
//
//    GroupsResponse rb = new GroupsResponse();
//
//    try {
//      service.modifyGroup(userName, groupId, body);
//      rb.setMessage("Group '" + groupId + "' has been successfully modified.");
//      return new ResponseEntity<>(rb, HttpStatus.OK);
//    } catch (ItemDoNotExistsException e1) {
//      return customExceptionFormatter.customException(rb, e1, HttpStatus.NOT_FOUND);
//    } catch (GroupAlreadyExistsException e2) {
//      return customExceptionFormatter.customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
//    } catch (Exception ee) {
//      return customExceptionFormatter.defaultException(rb, ee);
//    }
//  }
}
