package pl.databucket.server.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.databucket.server.dto.AccessTreeBucketDto;
import pl.databucket.server.dto.AccessTreeDto;
import pl.databucket.server.dto.AccessTreeGroupDto;
import pl.databucket.server.dto.AccessTreeViewDto;
import pl.databucket.server.dto.AuthProjectDTO;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.Group;
import pl.databucket.server.entity.User;
import pl.databucket.server.entity.View;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class AccessTreeService {

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private BucketService bucketService;

    @Autowired
    private ViewService viewService;

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Builds the whole tree in one read-only transaction: the access checks and the mapping walk
     * lazy relations of the user, groups, buckets and views.
     */
    @Transactional(readOnly = true)
    public AccessTreeDto getAccessTree(String username) {
        AccessTreeDto accessTreeDto = new AccessTreeDto();

        User user = userService.getUserByUsername(username);

        List<AuthProjectDTO> projects = user.getProjects().stream().map(item -> modelMapper.map(item, AuthProjectDTO.class)).collect(Collectors.toList());
        accessTreeDto.setProjects(projects.stream().filter(project -> project.isEnabled() && !project.isExpired()).collect(Collectors.toList()));

        List<Group> groups = groupService.getAccessTreeGroups(user);
        accessTreeDto.setGroups(groups.stream().map(item -> modelMapper.map(item, AccessTreeGroupDto.class)).collect(Collectors.toList()));

        List<Bucket> buckets = bucketService.getAccessTreeBuckets(user);
        accessTreeDto.setBuckets(buckets.stream().map(item -> modelMapper.map(item, AccessTreeBucketDto.class)).collect(Collectors.toList()));

        List<View> views = viewService.getAccessTreeViews(user);
        accessTreeDto.setViews(views.stream().map(item -> modelMapper.map(item, AccessTreeViewDto.class)).collect(Collectors.toList()));

        return cleanAccessTree(accessTreeDto);
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
}
