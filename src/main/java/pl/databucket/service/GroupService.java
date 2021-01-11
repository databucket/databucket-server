package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.dto.GroupDto;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.Group;
import pl.databucket.exception.ItemAlreadyExistsException;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.exception.SomeItemsNotFoundException;
import pl.databucket.repository.BucketRepository;
import pl.databucket.repository.GroupRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private BucketRepository bucketRepository;

    public Group createGroup(GroupDto groupDto) throws ItemAlreadyExistsException, SomeItemsNotFoundException {
        Group group = new Group();
        group.setName(groupDto.getName());
        group.setDescription(groupDto.getDescription());

        if (groupDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, groupDto.getBucketsIds());
            if (groupDto.getBucketsIds().size() != buckets.size()) {
                List<Long> foundIds = buckets.stream().map(Bucket::getId).collect(Collectors.toList());
                List<Long> givenIds = new ArrayList<>(groupDto.getBucketsIds());
                givenIds.removeAll(foundIds);
                throw new SomeItemsNotFoundException(Bucket.class, givenIds);
            } else
                group.setBuckets(new HashSet<>(buckets));
        }

        return groupRepository.save(group);
    }

    public Page<Group> getGroups(Specification<Group> specification, Pageable pageable) {
        return groupRepository.findAll(specification, pageable);
    }

    public Group modifyGroup(GroupDto groupDto) throws ItemNotFoundException, SomeItemsNotFoundException, ModifyByNullEntityIdException {
        if (groupDto.getId() == null)
            throw new ModifyByNullEntityIdException(Group.class);

        Group group = groupRepository.findByIdAndDeleted(groupDto.getId(), false);

        if (group == null)
            throw new ItemNotFoundException(Group.class, groupDto.getId());

        group.setName(groupDto.getName());
        group.setDescription(groupDto.getDescription());

        if (groupDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, groupDto.getBucketsIds());
            if (groupDto.getBucketsIds().size() != buckets.size()) {
                List<Long> foundIds = buckets.stream().map(Bucket::getId).collect(Collectors.toList());
                List<Long> givenIds = new ArrayList<>(groupDto.getBucketsIds());
                givenIds.removeAll(foundIds);
                throw new SomeItemsNotFoundException(Bucket.class, givenIds);
            }
            group.setBuckets(new HashSet<>(buckets));
        } else
            group.setBuckets(null);

        return groupRepository.save(group);
    }

    public void deleteGroup(Long groupId) throws ItemNotFoundException {
        Group group = groupRepository.findByIdAndDeleted(groupId, false);

        if (group == null)
            throw new ItemNotFoundException(Group.class, groupId);

        group.setDeleted(true);
        groupRepository.save(group);
    }
}
