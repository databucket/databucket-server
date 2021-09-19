package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.GroupDto;
import pl.databucket.server.entity.*;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TeamRepository teamRepository;


    public Group createGroup(GroupDto groupDto) throws ItemAlreadyExistsException, SomeItemsNotFoundException {
        Group group = new Group();
        group.setName(groupDto.getName());
        group.setShortName(groupDto.getShortName());
        group.setDescription(groupDto.getDescription());
        groupRepository.saveAndFlush(group);

        if (groupDto.getRoleId() != null) {
            Role role = roleRepository.getOne(groupDto.getRoleId());
            group.setRole(role);
        }

        if (groupDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, groupDto.getBucketsIds());
            group.setBuckets(new HashSet<>(buckets));
        }

        if (groupDto.getUsersIds() != null && groupDto.getUsersIds().size() > 0) {
            List<User> users = userRepository.findAllByIdIn(groupDto.getUsersIds());
            group.setUsers(new HashSet<>(users));
        }

        if (groupDto.getTeamsIds() != null && groupDto.getTeamsIds().size() > 0) {
            List<Team> teams = teamRepository.findAllByIdIn(groupDto.getTeamsIds());
            group.setTeams(new HashSet<>(teams));
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
        group.setShortName(groupDto.getShortName());
        group.setDescription(groupDto.getDescription());

        if (groupDto.getRoleId() != null) {
            Role role = roleRepository.getOne(groupDto.getRoleId());
            group.setRole(role);
        } else
            group.setRole(null);

        if (groupDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, groupDto.getBucketsIds());
            group.setBuckets(new HashSet<>(buckets));
        } else
            group.setBuckets(null);

        if (groupDto.getUsersIds() != null && groupDto.getUsersIds().size() > 0) {
            List<User> users = userRepository.findAllByIdIn(groupDto.getUsersIds());
            group.setUsers(new HashSet<>(users));
        } else
            group.setUsers(null);

        if (groupDto.getTeamsIds() != null && groupDto.getTeamsIds().size() > 0) {
            List<Team> teams = teamRepository.findAllByIdIn(groupDto.getTeamsIds());
            group.setTeams(new HashSet<>(teams));
        } else
            group.setTeams(null);

        return groupRepository.save(group);
    }

    public void deleteGroup(Long groupId) throws ItemNotFoundException {
        Group group = groupRepository.findByIdAndDeleted(groupId, false);

        if (group == null)
            throw new ItemNotFoundException(Group.class, groupId);

        group.setBuckets(null);
        group.setUsers(null);
        group.setTeams(null);

        group.setDeleted(true);
        groupRepository.save(group);
    }

    public List<Group> getAccessTreeGroups(User user) {
        return groupRepository.findAllByDeletedOrderById(false).stream().filter(group -> hasUserAccessToGroup(group, user)).collect(Collectors.toList());
    }

    private boolean hasUserAccessToGroup(Group group, User user) {
        boolean accessForUser = group.getUsers().size() > 0 && group.getUsers().contains(user);

        if (accessForUser)
            return true;
        else {
            boolean accessByRole = group.getRole() != null ? user.getRoles().contains(group.getRole()) : group.getTeams().size() > 0;
            boolean accessByTeam = group.getTeams().size() > 0 ? !Collections.disjoint(group.getTeams(), user.getTeams()) : group.getRole() != null;
            return accessByRole && accessByTeam;
        }
    }
}
