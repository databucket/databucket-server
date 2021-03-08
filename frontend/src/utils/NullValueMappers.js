export const getManageProjectMapper = () => {
    return {description: ''};
}

export const getManageUserMapper = () => {
    return {rolesIds: [], projectsIds: []};
}

export const getUserMapper = () => {
    return {rolesIds: [], teamsIds: []};
}

export const getGroupMapper = () => {
    return {name: '', description: '', bucketsIds: [], usersIds: [], teamsIds: [], roleId: 0};
}

export const getTeamMapper = () => {
    return {name: '', description: '', usersIds: []};
}

export const getBucketMapper = () => {
    return {description: '', groupsIds: [], usersIds: [], teamsIds: [], classId: 'none', roleId: 0};
}

export const getClassMapper = () => {
    return {description: ''};
}

export const getColumnsMapper = () => {
    return {description: '', configuration: []};
}

export const getFiltersMapper = () => {
    return {description: '', configuration: []};
}

export const getTasksMapper = () => {
    return {description: ''};
}

export const getEventsMapper = () => {
    return {description: ''};
}

export const getViewsMapper = () => {
    return {description: '', roleId: 0};
}

export const getColumnMapper = () => {
    return {width: '', format: '', enumId: -1};
}

export const getConditionsMapper = () => {
    return {};
}

export const getTagMapper = () => {
    return {description: '', bucketsIds: [], classesIds: []};
}

export const getEnumMapper = () => {
    return {description: '', items: []};
}