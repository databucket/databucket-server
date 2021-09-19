package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.TeamDto;
import pl.databucket.server.entity.*;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.repository.*;

import java.util.HashSet;
import java.util.List;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ViewRepository viewRepository;

    private final Misc misc = new Misc();

    public Team createTeam(TeamDto teamDto) throws ItemAlreadyExistsException, SomeItemsNotFoundException {
        Team team = new Team();
        team.setName(teamDto.getName());
        team.setDescription(teamDto.getDescription());
        teamRepository.saveAndFlush(team);

        if (teamDto.getUsersIds() != null && teamDto.getUsersIds().size() > 0) {
            List<User> users = userRepository.findAllByIdIn(teamDto.getUsersIds());
            for (User user : users) {
                user.getTeams().add(team);
                userRepository.save(user);
            }
            team.setUsers(new HashSet<>(users));
        }

        return teamRepository.save(team);
    }

    public List<Team> getTeams() {
        return teamRepository.findAllByDeletedOrderById(false);
    }

    public Team modifyTeam(TeamDto teamDto) throws ItemNotFoundException, SomeItemsNotFoundException, ModifyByNullEntityIdException {
        if (teamDto.getId() == null)
            throw new ModifyByNullEntityIdException(Team.class);

        Team team = teamRepository.findByIdAndDeleted(teamDto.getId(), false);

        if (team == null)
            throw new ItemNotFoundException(Team.class, teamDto.getId());

        team.setName(teamDto.getName());
        team.setDescription(teamDto.getDescription());

        if (!misc.equalsSetOfIds(teamDto.getUsersIds(), team.getUsersIds())) {
            for (User user : team.getUsers()) {
                user.getTeams().remove(team);
                userRepository.save(user);
            }
            List<User> users = userRepository.findAllByIdIn(teamDto.getUsersIds());
            if (users.size() > 0)
                for (User user : users) {
                    user.getTeams().add(team);
                    userRepository.save(user);
                }
            team.setUsers(new HashSet<>(users));
        }

        return teamRepository.save(team);
    }

    public void deleteTeam(short teamId) throws ItemNotFoundException {
        Team team = teamRepository.findByIdAndDeleted(teamId, false);

        if (team == null)
            throw new ItemNotFoundException(Team.class, teamId);

        for (User user : team.getUsers()) {
            user.getTeams().remove(team);
            userRepository.save(user);
        }

        for (Bucket bucket : team.getBuckets()) {
            bucket.getTeams().remove(team);
            bucketRepository.save(bucket);
        }

        for (Group group : team.getGroups()) {
            group.getTeams().remove(team);
            groupRepository.save(group);
        }

        for (View view : team.getViews()) {
            view.getTeams().remove(team);
            viewRepository.save(view);
        }

        team.setDeleted(true);
        teamRepository.save(team);
    }
}
