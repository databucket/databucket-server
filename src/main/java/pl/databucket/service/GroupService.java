package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.dto.GroupDto;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.Group;
import pl.databucket.entity.User;
import pl.databucket.exception.*;
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
    private BucketRepository bucketRepository;

    @Autowired
    private UserRepository userRepository;

    public Group createGroup(GroupDto groupDto) throws GroupAlreadyExistsException {
        if (groupRepository.existsByNameAndDeleted(groupDto.getName(), false))
            throw new GroupAlreadyExistsException(groupDto.getName());

        Group group = new Group();
        group.setName(groupDto.getName());
        group.setDescription(groupDto.getDescription());

        if (groupDto.getBuckets() != null) {
            List<Bucket> buckets = bucketRepository.findAllById(groupDto.getBuckets());
            group.setBuckets(new HashSet<>(buckets));
        }

        if (groupDto.getUsers() != null) {
            List<User> users = userRepository.findAllById(groupDto.getUsers());
            group.setUsers(new HashSet<>(users));
        }

        return groupRepository.save(group);
    }

    public Page<Group> getGroups(Specification<Group> specification, Pageable pageable) {
        return groupRepository.findAll(specification, pageable);
    }

    public void deleteGroup(Long groupId) {
        Group group = groupRepository.getOne(groupId);
        group.setDeleted(true);
        groupRepository.save(group);
    }

    public Group modifyGroup(GroupDto groupDto) {
        Group group = groupRepository.getOne(groupDto.getId());
        group.setName(groupDto.getName());
        group.setDescription(groupDto.getDescription());

        if (groupDto.getBuckets() != null) {
            List<Bucket> buckets = bucketRepository.findAllById(groupDto.getBuckets());
            group.setBuckets(new HashSet<>(buckets));
        } else
            group.setBuckets(null);

        if (groupDto.getUsers() != null) {
            List<User> users = userRepository.findAllById(groupDto.getUsers());
            group.setUsers(new HashSet<>(users));
        } else
            group.setUsers(null);

        return groupRepository.save(group);
    }
}
