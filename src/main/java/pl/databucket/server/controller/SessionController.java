package pl.databucket.server.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.dto.AccessTreeBucketDto;
import pl.databucket.server.dto.AccessTreeDto;
import pl.databucket.server.dto.AccessTreeGroupDto;
import pl.databucket.server.dto.AccessTreeViewDto;
import pl.databucket.server.dto.AuthProjectDTO;
import pl.databucket.server.dto.DataFilterDto;
import pl.databucket.server.dto.TaskDto;
import pl.databucket.server.dto.UserColumnsDto;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.DataColumns;
import pl.databucket.server.entity.DataFilter;
import pl.databucket.server.entity.Group;
import pl.databucket.server.entity.Task;
import pl.databucket.server.entity.User;
import pl.databucket.server.entity.View;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.service.BucketService;
import pl.databucket.server.service.DataColumnsService;
import pl.databucket.server.service.DataFilterService;
import pl.databucket.server.service.GroupService;
import pl.databucket.server.service.TaskService;
import pl.databucket.server.service.UserService;
import pl.databucket.server.service.ViewService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/session")
@RestController
@RequiredArgsConstructor
public class SessionController {

    private final UserService userService;
    private final GroupService groupService;
    private final BucketService bucketService;
    private final ViewService viewService;
    private final DataColumnsService columnsService;
    private final DataFilterService filterService;
    private final TaskService taskService;
    private final ModelMapper modelMapper;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(SessionController.class);


    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @GetMapping(value = "/access-tree")
    public ResponseEntity<?> getAccessTree(Authentication authentication) {
        try {
            AccessTreeDto accessTreeDto = new AccessTreeDto();
            User user = userService.getUserByUsername(authentication.getName());

            List<AuthProjectDTO> projects = user.getProjects().stream()
                .map(item -> modelMapper.map(item, AuthProjectDTO.class)).toList();
            accessTreeDto.setProjects(
                projects.stream().filter(project -> project.isEnabled() && !project.isExpired()).toList());

            List<Group> groups = groupService.getAccessTreeGroups(user);
            accessTreeDto.setGroups(
                groups.stream().map(item -> modelMapper.map(item, AccessTreeGroupDto.class)).toList());

            List<Bucket> buckets = bucketService.getAccessTreeBuckets(user);
            accessTreeDto.setBuckets(
                buckets.stream().map(item -> modelMapper.map(item, AccessTreeBucketDto.class)).toList());

            List<View> views = viewService.getAccessTreeViews(user);
            accessTreeDto.setViews(views.stream().map(item -> modelMapper.map(item, AccessTreeViewDto.class)).toList());

            return ResponseEntity.ok(cleanAccessTree(accessTreeDto));
        } catch (IllegalArgumentException e1) {
            return exceptionFormatter.customException(e1, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    /**
     * Remove items from group.bucketsIds that not exist in buckets array
     * Create orphaned buckets group if there is some orphaned buckets and number of groups is more than 0
     */
    private AccessTreeDto cleanAccessTree(AccessTreeDto accessTreeDto) {
        if (!accessTreeDto.getGroups().isEmpty()) {
            Set<Long> allUserBucketsIds = accessTreeDto.getBuckets().stream().map(AccessTreeBucketDto::getId)
                .collect(Collectors.toSet());
            Set<Long> allGroupsBucketsIds = new HashSet<>();

            accessTreeDto.getGroups().forEach(group -> {
                if (group.getBucketsIds() != null) {
                    Set<Long> checkedIds = group.getBucketsIds().stream().filter(allUserBucketsIds::contains)
                        .collect(Collectors.toSet());
                    group.setBucketsIds(checkedIds);
                    allGroupsBucketsIds.addAll(group.getBucketsIds());
                }
            });

            allUserBucketsIds.removeAll(allGroupsBucketsIds);
            if (!allUserBucketsIds.isEmpty()) {
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
    @GetMapping(value = "/columns/{ids}")
    public ResponseEntity<?> getUserColumns(@PathVariable List<Long> ids) {
        try {
            List<DataColumns> dataColumns = columnsService.getColumns(ids);
            List<UserColumnsDto> dataColumnsDto = dataColumns.stream()
                .map(item -> modelMapper.map(item, UserColumnsDto.class)).toList();
            return ResponseEntity.ok(dataColumnsDto);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @GetMapping(value = "/filters/{ids}")
    public ResponseEntity<?> getUserFilters(@PathVariable List<Long> ids) {
        try {
            List<DataFilter> dataFilters = filterService.getFilters(ids);
            List<DataFilterDto> dataFiltersDto = dataFilters.stream()
                .map(item -> modelMapper.map(item, DataFilterDto.class)).toList();
            return ResponseEntity.ok(dataFiltersDto);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @GetMapping(value = "/tasks/{ids}")
    public ResponseEntity<?> getUserTasks(@PathVariable List<Long> ids) {
        try {
            List<Task> tasks = taskService.getTasks(ids);
            List<TaskDto> tasksDto = tasks.stream().map(item -> modelMapper.map(item, TaskDto.class)).toList();
            return ResponseEntity.ok(tasksDto);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

}
