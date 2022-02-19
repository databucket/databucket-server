package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.ViewDto;
import pl.databucket.server.entity.*;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class ViewService {

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    @Autowired
    private DataColumnsRepository columnsRepository;

    @Autowired
    private DataFilterRepository dataFilterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TeamRepository teamRepository;

    public View createView(ViewDto viewDto) throws ItemNotFoundException {
        View view = new View();
        view.setName(viewDto.getName());
        view.setDescription(viewDto.getDescription());
        view.setFeaturesIds(viewDto.getFeaturesIds());

        if (viewDto.getRoleId() != null) {
            Role role = roleRepository.getById(viewDto.getRoleId());
            view.setRole(role);
        }

        DataColumns dataColumns = columnsRepository.findByIdAndDeleted(viewDto.getColumnsId(), false);
        if (dataColumns != null)
            view.setDataColumns(dataColumns);
        else
            throw new ItemNotFoundException(DataColumns.class, viewDto.getColumnsId());

        viewRepository.saveAndFlush(view);

        if (viewDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, viewDto.getBucketsIds());
            view.setBuckets(new HashSet<>(buckets));
        }

        if (viewDto.getClassesIds() != null) {
            List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndIdIn(false, viewDto.getClassesIds());
            view.setDataClasses(new HashSet<>(dataClasses));
        }

        if (viewDto.getFilterId() != null) {
            DataFilter dataFilter = dataFilterRepository.findByIdAndDeleted(viewDto.getFilterId(), false);
            if (dataFilter != null)
                view.setDataFilter(dataFilter);
            else
                throw new ItemNotFoundException(DataFilter.class, viewDto.getFilterId());
        }

        if (viewDto.getUsersIds() != null && viewDto.getUsersIds().size() > 0) {
            List<User> users = userRepository.findAllByIdIn(viewDto.getUsersIds());
            view.setUsers(new HashSet<>(users));
        }

        if (viewDto.getTeamsIds() != null && viewDto.getTeamsIds().size() > 0) {
            List<Team> teams = teamRepository.findAllByIdIn(viewDto.getTeamsIds());
            view.setTeams(new HashSet<>(teams));
        }

        return viewRepository.save(view);
    }

    public List<View> getViews() {
        return viewRepository.findAllByDeletedOrderById(false);
    }

    public List<View> getViews(List<Long> ids) {
        return viewRepository.findAllByDeletedAndIdIn(false, ids);
    }

    public View modifyView(ViewDto viewDto) throws ItemNotFoundException, ModifyByNullEntityIdException {
        if (viewDto.getId() == null)
            throw new ModifyByNullEntityIdException(View.class);

        View view = viewRepository.findByIdAndDeleted(viewDto.getId(), false);

        if (view == null)
            throw new ItemNotFoundException(View.class, viewDto.getId());

        view.setName(viewDto.getName());
        view.setDescription(viewDto.getDescription());
        view.setFeaturesIds(viewDto.getFeaturesIds());

        if (viewDto.getRoleId() != null) {
            Role role = roleRepository.getOne(viewDto.getRoleId());
            view.setRole(role);
        } else
            view.setRole(null);

        if (viewDto.getBucketsIds() != null && viewDto.getBucketsIds().size() > 0) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, viewDto.getBucketsIds());
            view.setBuckets(new HashSet<>(buckets));
        } else
            view.setBuckets(null);

        if (viewDto.getClassesIds() != null && viewDto.getClassesIds().size() > 0) {
            List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndIdIn(false, viewDto.getClassesIds());
            view.setDataClasses(new HashSet<>(dataClasses));
        } else
            view.setDataClasses(null);

        DataColumns dataColumns = columnsRepository.findByIdAndDeleted(viewDto.getColumnsId(), false);
        if (dataColumns != null)
            view.setDataColumns(dataColumns);
        else
            throw new ItemNotFoundException(DataColumns.class, viewDto.getColumnsId());

        if (viewDto.getFilterId() != null) {
            DataFilter dataFilter = dataFilterRepository.findByIdAndDeleted(viewDto.getFilterId(), false);
            if (dataFilter != null)
                view.setDataFilter(dataFilter);
            else
                throw new ItemNotFoundException(DataFilter.class, viewDto.getFilterId());
        } else
            view.setDataFilter(null);

        if (viewDto.getUsersIds() != null && viewDto.getUsersIds().size() > 0) {
            List<User> users = userRepository.findAllByIdIn(viewDto.getUsersIds());
            view.setUsers(new HashSet<>(users));
        } else
            view.setUsers(null);

        if (viewDto.getTeamsIds() != null && viewDto.getTeamsIds().size() > 0) {
            List<Team> teams = teamRepository.findAllByIdIn(viewDto.getTeamsIds());
            view.setTeams(new HashSet<>(teams));
        } else
            view.setTeams(null);

        return viewRepository.save(view);
    }

    public void deleteView(long viewId) throws ItemNotFoundException {
        View view = viewRepository.findByIdAndDeleted(viewId, false);

        if (view == null)
            throw new ItemNotFoundException(View.class, viewId);

        view.setBuckets(null);
        view.setUsers(null);
        view.setTeams(null);
        view.setDataClasses(null);

        view.setDeleted(true);
        viewRepository.save(view);
    }

    public List<View> getAccessTreeViews(User user) {
        return viewRepository.findAllByDeletedOrderById(false).stream().filter(view -> hasUserAccessToView(view, user)).collect(Collectors.toList());
    }

    private boolean hasUserAccessToView(View view, User user) {
        boolean accessForUser = view.getUsers().size() > 0 && view.getUsers().contains(user);

        if (accessForUser)
            return true;
        else {
            boolean accessByRole = view.getRole() != null ? user.getRoles().contains(view.getRole()) : view.getTeams().size() > 0;
            boolean accessByTeam = view.getTeams().size() > 0 ? !Collections.disjoint(view.getTeams(), user.getTeams()) : view.getRole() != null;
            return accessByRole && accessByTeam;
        }
    }
}
