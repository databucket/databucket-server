package pl.databucket.server.service.template;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.*;
import pl.databucket.server.entity.*;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.repository.*;
import pl.databucket.server.service.*;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class TemplateService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private DataEnumService dataEnumService;

    @Autowired
    private DataClassService dataClassService;

    @Autowired
    private DataClassRepository dataClassRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupService groupService;

    @Autowired
    private TagService tagService;

    @Autowired
    private BucketService bucketService;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private DataColumnsService dataColumnsService;

    @Autowired
    private DataColumnsRepository dataColumnsRepository;

    @Autowired
    private DataFilterService dataFilterService;

    @Autowired
    private DataFilterRepository dataFilterRepository;

    @Autowired
    private ViewService viewService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ModelMapper modelMapper;


    public void runTemplate(int id) throws ItemNotFoundException, ItemAlreadyExistsException, SomeItemsNotFoundException {
        Optional<Template> template = templateRepository.findById(id);
        if (!template.isPresent())
            throw new ItemNotFoundException(Template.class, id);

        TemplateConfDto configuration = template.get().getConfiguration();

        // Create teams
        List<Map<String, Object>> teams = configuration.getTeams();
        if (teams != null && teams.size() > 0)
            for(Map<String, Object> team: teams)
                teamService.createTeam(createTeamDto(team));

        // Create enums
        List<Map<String, Object>> enums = configuration.getEnums();
        if (enums != null && enums.size() > 0)
            for(Map<String, Object> e: enums)
                dataEnumService.createDataEnum(createDataEnumDto(e));

        // Create class
        List<Map<String, Object>> classes = configuration.getClasses();
        if (classes != null && classes.size() > 0)
            for(Map<String, Object> c: classes)
                dataClassService.createDataClass(createDataClassDto(c));

        // Create groups
        List<Map<String, Object>> groups = configuration.getGroups();
        if (groups != null && groups.size() > 0)
            for(Map<String, Object> group: groups)
                groupService.createGroup(createGroupDto(group));

        // Create buckets
        List<Map<String, Object>> buckets = configuration.getBuckets();
        if (buckets != null && buckets.size() > 0)
            for(Map<String, Object> bucket: buckets)
                bucketService.createBucket(createBucketDto(bucket));

        // Create tags
        List<Map<String, Object>> tags = configuration.getTags();
        if (tags != null && tags.size() > 0)
            for(Map<String, Object> tag: tags)
                tagService.createTag(createTagDto(tag));

        // Create columns
        List<Map<String, Object>> columnsList = configuration.getColumns();
        if (columnsList != null && columnsList.size() > 0)
            for(Map<String, Object> columnsMap: columnsList)
                dataColumnsService.createColumns(createColumnsDto(columnsMap));

        // Create filters
        List<Map<String, Object>> filters = configuration.getFilters();
        if (filters != null && filters.size() > 0)
            for(Map<String, Object> filter: filters)
                dataFilterService.createFilter(createFilterDto(filter));

        // Create views
        List<Map<String, Object>> views = configuration.getViews();
        if (views != null && views.size() > 0)
            for(Map<String, Object> view: views)
                viewService.createView(createViewDto(view));

        // Create tasks
        List<Map<String, Object>> tasks = configuration.getTasks();
        if (tasks != null && tasks.size() > 0)
            for(Map<String, Object> task: tasks)
                taskService.createTask(createTaskDto(task));

    }

    public Template createTemplate(TemplateDto templateDto) throws ItemNotFoundException {
        Template template = new Template();

        template.setName(templateDto.getName());
        template.setDescription(templateDto.getDescription());
        template.setConfiguration(templateDto.getConfiguration());

        if (templateDto.getTemplatesIds() != null) {
            List<Template> templates = templateRepository.findAllByIdInOrderById(templateDto.getTemplatesIds());
            template.setTemplates(new HashSet<>(templates));
        }

        if (templateDto.getProjectsIds() != null) {
            List<Project> projects = projectRepository.findAllByDeletedAndIdIn(false, templateDto.getProjectsIds());
            template.setProjects(new HashSet<>(projects));
        }

        return templateRepository.save(template);
    }

    public List<Template> getTemplates() {
        return templateRepository.findAllByOrderByIdAsc();
    }

    public List<Template> getTemplates(int projectId) throws ItemNotFoundException {
        Project project = projectRepository.findByIdAndDeleted(projectId, false);

        if (project == null)
            throw new ItemNotFoundException(Project.class, projectId);

        return templateRepository.findAllByProjectsContainsOrderByIdAsc(project);
    }

    public Template modifyTemplate(TemplateDto templateDto) throws ItemNotFoundException, ModifyByNullEntityIdException {
        if (templateDto.getId() == null)
            throw new ModifyByNullEntityIdException(Template.class);

        Optional<Template> templateOpt = templateRepository.findById(templateDto.getId());
        if (!templateOpt.isPresent())
            throw new ItemNotFoundException(Template.class, templateDto.getId());

        Template template = templateOpt.get();

        template.setName(templateDto.getName());
        template.setDescription(templateDto.getDescription());
        template.setConfiguration(templateDto.getConfiguration());

        if (templateDto.getTemplatesIds() != null) {
            List<Template> templates = templateRepository.findAllByIdInOrderById(templateDto.getTemplatesIds());
            template.setTemplates(new HashSet<>(templates));
        }

        if (templateDto.getProjectsIds() != null) {
            List<Project> projects = projectRepository.findAllByDeletedAndIdIn(false, templateDto.getProjectsIds());
            template.setProjects(new HashSet<>(projects));
        }

        return templateRepository.save(template);
    }

    public void deleteTemplate(int templateId) throws ItemNotFoundException {
        Optional<Template> templateOpt = templateRepository.findById(templateId);
        if (!templateOpt.isPresent())
            throw new ItemNotFoundException(Template.class, templateId);

        Template template = templateOpt.get();
        template.setProjects(null); // TODO: check if this is required
        template.setTemplates(null);

        templateRepository.delete(template);
    }

    private TeamDto createTeamDto(Map<String, Object> teamMap) {
        TeamDto teamDto = new TeamDto();

        if (teamMap.containsKey(Const.NAME))
            teamDto.setName((String) teamMap.get(Const.NAME));

        if (teamMap.containsKey(Const.DESCRIPTION))
            teamDto.setDescription((String) teamMap.get(Const.DESCRIPTION));

        if (teamMap.containsKey(Const.USER_NAMES))
            teamDto.setUsersIds(getUsersIds((List<String>) teamMap.get(Const.USER_NAMES)));

        return teamDto;
    }

    private DataEnumDto createDataEnumDto(Map<String, Object> enumMap) {
        return modelMapper.map(enumMap, DataEnumDto.class);
    }

    private DataClassDto createDataClassDto(Map<String, Object> classMap) {
        return modelMapper.map(classMap, DataClassDto.class);
    }

    private GroupDto createGroupDto(Map<String, Object> groupMap) {
        GroupDto groupDto = new GroupDto();

        if (groupMap.containsKey(Const.SHORT_NAME))
            groupDto.setShortName((String) groupMap.get(Const.SHORT_NAME));

        if (groupMap.containsKey(Const.NAME))
            groupDto.setName((String) groupMap.get(Const.NAME));

        if (groupMap.containsKey(Const.DESCRIPTION))
            groupDto.setDescription((String) groupMap.get(Const.DESCRIPTION));

        if (groupMap.containsKey(Const.BUCKET_NAMES))
            groupDto.setBucketsIds(getBucketsIds((List<String>) groupMap.get(Const.BUCKET_NAMES)));

        if (groupMap.containsKey(Const.USER_NAMES))
            groupDto.setUsersIds(getUsersIds((List<String>) groupMap.get(Const.USER_NAMES)));

        if (groupMap.containsKey(Const.TEAM_NAMES))
            groupDto.setTeamsIds(getTeamsIds((List<String>) groupMap.get(Const.TEAM_NAMES)));

        if (groupMap.containsKey(Const.ROLE_NAME)) {
            Role role = roleRepository.findByName((String) groupMap.get(Const.ROLE_NAME));
            groupDto.setRoleId(role.getId());
        }

        return groupDto;
    }

    private BucketDto createBucketDto(Map<String, Object> bucketMap) {
        BucketDto bucketDto = new BucketDto();

//        if (bucketMap.containsKey(Const.ICON_NAME))
//            bucketDto.setIconName((String) bucketMap.get(Const.ICON_NAME));
//
//        if (bucketMap.containsKey(Const.ICON_COLOR))
//            bucketDto.setIconColor((String) bucketMap.get(Const.ICON_COLOR));

        if (bucketMap.containsKey(Const.NAME))
            bucketDto.setName((String) bucketMap.get(Const.NAME));

        if (bucketMap.containsKey(Const.DESCRIPTION))
            bucketDto.setDescription((String) bucketMap.get(Const.DESCRIPTION));

        if (bucketMap.containsKey(Const.CLASS_NAME)) {
            DataClass dataClass = dataClassRepository.findByNameAndDeleted((String) bucketMap.get(Const.CLASS_NAME), false);
            bucketDto.setClassId(dataClass.getId());
        }

        if (bucketMap.containsKey(Const.GROUP_NAMES))
            bucketDto.setGroupsIds(getGroupsIds((List<String>) bucketMap.get(Const.GROUP_NAMES)));

        bucketDto.setProtectedData(bucketMap.containsKey(Const.PROTECTED_DATA) && (Boolean) bucketMap.get(Const.PROTECTED_DATA));

        bucketDto.setHistory(bucketMap.containsKey(Const.HISTORY) && (Boolean) bucketMap.get(Const.HISTORY));

        if (bucketMap.containsKey(Const.USER_NAMES))
            bucketDto.setUsersIds(getUsersIds((List<String>) bucketMap.get(Const.USER_NAMES)));

        if (bucketMap.containsKey(Const.ROLE_NAME)) {
            Object roleIdentity = bucketMap.get(Const.ROLE_NAME);
            if (roleIdentity instanceof String) {
                Role role = roleRepository.findByName((String) roleIdentity);
                bucketDto.setRoleId(role.getId());
            } else
                bucketDto.setRoleId((Short) bucketMap.get(Const.ROLE_NAME));
        }

        if (bucketMap.containsKey(Const.TEAM_NAMES))
            bucketDto.setTeamsIds(getTeamsIds((List<String>) bucketMap.get(Const.TEAM_NAMES)));

        return bucketDto;
    }

    private TagDto createTagDto(Map<String, Object> tagMap) {
        TagDto tagDto = new TagDto();

        if (tagMap.containsKey(Const.NAME))
            tagDto.setName((String) tagMap.get(Const.NAME));

        if (tagMap.containsKey(Const.DESCRIPTION))
            tagDto.setDescription((String) tagMap.get(Const.DESCRIPTION));

        if (tagMap.containsKey(Const.BUCKET_NAMES))
            tagDto.setBucketsIds(getBucketsIds((List<String>) tagMap.get(Const.BUCKET_NAMES)));

        if (tagMap.containsKey(Const.CLASS_NAMES))
            tagDto.setClassesIds(getClassesIds((List<String>) tagMap.get(Const.CLASS_NAMES)));

        return tagDto;
    }

    private DataColumnsDto createColumnsDto(Map<String, Object> columnsMap) {
        DataColumnsDto dataColumnsDto = new DataColumnsDto();

        if (columnsMap.containsKey(Const.NAME))
            dataColumnsDto.setName((String) columnsMap.get(Const.NAME));

        if (columnsMap.containsKey(Const.DESCRIPTION))
            dataColumnsDto.setDescription((String) columnsMap.get(Const.DESCRIPTION));

        if (columnsMap.containsKey(Const.CLASS_NAME)) {
            DataClass dataClass = dataClassRepository.findByNameAndDeleted((String) columnsMap.get(Const.CLASS_NAME), false);
            dataColumnsDto.setClassId(dataClass.getId());
        }

        if (columnsMap.containsKey(Const.CONFIGURATION)) {
            DataColumnsConfigDto configuration = modelMapper.map(columnsMap.get(Const.CONFIGURATION), DataColumnsConfigDto.class);
            dataColumnsDto.setConfiguration(configuration);
        }

        return dataColumnsDto;
    }

    private DataFilterDto createFilterDto(Map<String, Object> filterMap) {
        DataFilterDto dataFilterDto = new DataFilterDto();

        if (filterMap.containsKey(Const.NAME))
            dataFilterDto.setName((String) filterMap.get(Const.NAME));

        if (filterMap.containsKey(Const.DESCRIPTION))
            dataFilterDto.setDescription((String) filterMap.get(Const.DESCRIPTION));

        if (filterMap.containsKey(Const.CLASS_NAME)) {
            DataClass dataClass = dataClassRepository.findByNameAndDeleted((String) filterMap.get(Const.CLASS_NAME), false);
            dataFilterDto.setClassId(dataClass.getId());
        }

        if (filterMap.containsKey(Const.CONFIGURATION)) {
            DataFilterConfigDto configuration = modelMapper.map(filterMap.get(Const.CONFIGURATION), DataFilterConfigDto.class);
            dataFilterDto.setConfiguration(configuration);
        }

        return dataFilterDto;
    }

    private ViewDto createViewDto(Map<String, Object> viewMap) {
        ViewDto viewDto = new ViewDto();

        if (viewMap.containsKey(Const.NAME))
            viewDto.setName((String) viewMap.get(Const.NAME));

        if (viewMap.containsKey(Const.DESCRIPTION))
            viewDto.setDescription((String) viewMap.get(Const.DESCRIPTION));

        if (viewMap.containsKey(Const.COLUMNS_NAME)) {
            DataColumns dataColumns = dataColumnsRepository.findByNameAndDeleted((String) viewMap.get(Const.COLUMNS_NAME), false);
            viewDto.setColumnsId(dataColumns.getId());
        }

        if (viewMap.containsKey(Const.FILTER_NAME)) {
            DataFilter dataFilter = dataFilterRepository.findByIdAndDeleted((Long) viewMap.get(Const.FILTER_NAME), false);
            viewDto.setFilterId(dataFilter.getId());
        }

        if (viewMap.containsKey(Const.BUCKET_NAMES))
            viewDto.setBucketsIds(getBucketsIds((List<String>) viewMap.get(Const.BUCKET_NAMES)));

        if (viewMap.containsKey(Const.CLASS_NAMES))
            viewDto.setClassesIds(getClassesIds((List<String>) viewMap.get(Const.CLASS_NAMES)));

        if (viewMap.containsKey(Const.USER_NAMES))
            viewDto.setUsersIds(getUsersIds((List<String>) viewMap.get(Const.USER_NAMES)));

        if (viewMap.containsKey(Const.ROLE_NAME)) {
            Role role = roleRepository.findByName((String) viewMap.get(Const.ROLE_NAME));
            viewDto.setRoleId(role.getId());
        }

        if (viewMap.containsKey(Const.TEAM_NAMES))
            viewDto.setTeamsIds(getTeamsIds((List<String>) viewMap.get(Const.TEAM_NAMES)));

        if (viewMap.containsKey(Const.FEATURES_IDS))
            viewDto.setFeaturesIds((Short[]) viewMap.get(Const.FEATURES_IDS));

        return viewDto;
    }

    private TaskDto createTaskDto(Map<String, Object> taskMap) {
        TaskDto taskDto = new TaskDto();

        if (taskMap.containsKey(Const.NAME))
            taskDto.setName((String) taskMap.get(Const.NAME));

        if (taskMap.containsKey(Const.DESCRIPTION))
            taskDto.setDescription((String) taskMap.get(Const.DESCRIPTION));

        if (taskMap.containsKey(Const.CLASS_NAME)) {
            DataClass dataClass = dataClassRepository.findByNameAndDeleted((String) taskMap.get(Const.CLASS_NAME), false);
            taskDto.setClassId(dataClass.getId());
        }

        if (taskMap.containsKey(Const.FILTER_NAME)) {
            DataFilter dataFilter = dataFilterRepository.findByIdAndDeleted((Long) taskMap.get(Const.FILTER_NAME), false);
            taskDto.setFilterId(dataFilter.getId());
        }

        if (taskMap.containsKey(Const.BUCKET_NAMES))
            taskDto.setBucketsIds(getBucketsIds((List<String>) taskMap.get(Const.BUCKET_NAMES)));

        if (taskMap.containsKey(Const.CLASS_NAMES))
            taskDto.setClassesIds(getClassesIds((List<String>) taskMap.get(Const.CLASS_NAMES)));

        if (taskMap.containsKey(Const.CONFIGURATION)) {
            TaskConfigDto configuration = modelMapper.map(taskMap.get(Const.CONFIGURATION), TaskConfigDto.class);
            taskDto.setConfiguration(configuration);
        }

        return taskDto;
    }

    private Set<Long> getGroupsIds(List<String> groupNames) {
        List<Group> groups = groupRepository.findAllByDeletedAndNameIn(false, groupNames);
        return groups.stream().map(Group::getId).collect(Collectors.toSet());
    }

    private Set<Long> getBucketsIds(List<String> bucketNames) {
        List<Bucket> buckets = bucketRepository.findAllByDeletedAndNameIn(false, bucketNames);
        return buckets.stream().map(Bucket::getId).collect(Collectors.toSet());
    }

    private Set<Long> getUsersIds(List<String> userNames) {
        List<User> users = userRepository.findAllByUsernameIn(userNames);
        return users.stream().map(User::getId).collect(Collectors.toSet());
    }

    private Set<Short> getTeamsIds(List<String> teamNames) {
        List<Team> teams = teamRepository.findAllByDeletedAndNameIn(false, teamNames);
        return teams.stream().map(Team::getId).collect(Collectors.toSet());
    }

    private Set<Long> getClassesIds(List<String> classNames) {
        List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndNameIn(false, classNames);
        return dataClasses.stream().map(DataClass::getId).collect(Collectors.toSet());
    }

}
