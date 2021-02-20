package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.dto.GroupDto;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.Group;
import pl.databucket.entity.User;
import pl.databucket.exception.ItemAlreadyExistsException;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.exception.SomeItemsNotFoundException;
import pl.databucket.repository.BucketRepository;
import pl.databucket.repository.GroupRepository;
import pl.databucket.repository.UserRepository;
import java.util.HashSet;
import java.util.List;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BucketRepository bucketRepository;

    public Group createGroup(GroupDto groupDto) throws ItemAlreadyExistsException, SomeItemsNotFoundException {
        Group group = new Group();
        group.setName(groupDto.getName());
        group.setDescription(groupDto.getDescription());
        group.setPrivateItem(groupDto.isPrivateItem());
        groupRepository.saveAndFlush(group);

        if (groupDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, groupDto.getBucketsIds());
            group.setBuckets(new HashSet<>(buckets));
        }

        if (groupDto.getUsersIds() != null && groupDto.getUsersIds().size() > 0) {
            List<User> users = userRepository.findAllByIdIn(groupDto.getUsersIds());
            for (User user : users) {
                user.getGroups().add(group);
                userRepository.save(user);
            }
            group.setUsers(new HashSet<>(users));
        }

        return groupRepository.save(group);
    }

    public List<Group> getGroups() {
        return groupRepository.findAllByDeletedOrderById(false);
    }

    public Group modifyGroup(GroupDto groupDto) throws ItemNotFoundException, SomeItemsNotFoundException, ModifyByNullEntityIdException {
        if (groupDto.getId() == null)
            throw new ModifyByNullEntityIdException(Group.class);

        Group group = groupRepository.findByIdAndDeleted(groupDto.getId(), false);

        if (group == null)
            throw new ItemNotFoundException(Group.class, groupDto.getId());

        group.setName(groupDto.getName());
        group.setDescription(groupDto.getDescription());
        group.setPrivateItem(groupDto.isPrivateItem());

        if (groupDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, groupDto.getBucketsIds());
            group.setBuckets(new HashSet<>(buckets));
        }

        if (groupDto.getUsersIds() != null) {
            List<User> users = userRepository.findAllByIdIn(groupDto.getUsersIds());
            for (User user : users) {
                user.getGroups().add(group);
                userRepository.save(user);
            }
            group.setUsers(new HashSet<>(users));
        }

        return groupRepository.save(group);
    }

    public void deleteGroup(Long groupId) throws ItemNotFoundException {
        Group group = groupRepository.findByIdAndDeleted(groupId, false);

        if (group == null)
            throw new ItemNotFoundException(Group.class, groupId);

        group.setBuckets(null);

        for (User user : group.getUsers()) {
            user.getGroups().remove(group);
            userRepository.save(user);
        }

        group.setDeleted(true);
        groupRepository.save(group);
    }
}
