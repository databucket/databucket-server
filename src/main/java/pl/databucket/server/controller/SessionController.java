package pl.databucket.server.controller;

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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/session")
@RestController
public class SessionController {

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private BucketService bucketService;

    @Autowired
    private ViewService viewService;

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
            AccessTreeDto accessTreeDto = new AccessTreeDto();

            User user = userService.getUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());

            List<AuthProjectDTO> projects = user.getProjects().stream().map(item -> modelMapper.map(item, AuthProjectDTO.class)).collect(Collectors.toList());
            accessTreeDto.setProjects(projects.stream().filter(project -> project.isEnabled() && !project.isExpired()).collect(Collectors.toList()));

            List<Group> groups = groupService.getAccessTreeGroups(user);
            accessTreeDto.setGroups(groups.stream().map(item -> modelMapper.map(item, AccessTreeGroupDto.class)).collect(Collectors.toList()));

            List<Bucket> buckets = bucketService.getAccessTreeBuckets(user);
            accessTreeDto.setBuckets(buckets.stream().map(item -> modelMapper.map(item, AccessTreeBucketDto.class)).collect(Collectors.toList()));

            List<View> views = viewService.getAccessTreeViews(user);
            accessTreeDto.setViews(views.stream().map(item -> modelMapper.map(item, AccessTreeViewDto.class)).collect(Collectors.toList()));

            return new ResponseEntity<>(cleanAccessTree(accessTreeDto), HttpStatus.OK);
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    /**
     * Remove items from group.bucketsIds that not exist in buckets array
     * Create orphaned buckets group if there is some orphaned buckets and number of groups is more than 0
     */
    private AccessTreeDto cleanAccessTree(AccessTreeDto accessTreeDto) {
        if (accessTreeDto.getGroups().size() > 0) {
            Set<Long> allUserBucketsIds = accessTreeDto.getBuckets().stream().map(AccessTreeBucketDto::getId).collect(Collectors.toSet());
            Set<Long> allGroupsBucketsIds = new HashSet<>();

            accessTreeDto.getGroups().stream().forEach(group -> {
                if (group.getBucketsIds() != null) {
                    Set<Long> checkedIds = group.getBucketsIds().stream().filter(allUserBucketsIds::contains).collect(Collectors.toSet());
                    group.setBucketsIds(checkedIds);
                    allGroupsBucketsIds.addAll(group.getBucketsIds());
                }
            });

            allUserBucketsIds.removeAll(allGroupsBucketsIds);
            if (allUserBucketsIds.size() > 0) {
                AccessTreeGroupDto orphanedBucketsGroupDto = new AccessTreeGroupDto();
                orphanedBucketsGroupDto.setId(0L);
                orphanedBucketsGroupDto.setShortName("ORP");
                orphanedBucketsGroupDto.setName("Orphaned");
                orphanedBucketsGroupDto.setBucketsIds(allUserBucketsIds);
                accessTreeDto.getGroups().add(orphanedBucketsGroupDto);
            }
        }

        return accessTreeDto;
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
            List<Task> tasks = taskService.getTasks(ids);
            List<TaskDto> tasksDto = tasks.stream().map(item -> modelMapper.map(item, TaskDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(tasksDto, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

}
