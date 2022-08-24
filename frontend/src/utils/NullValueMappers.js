export const getManageProjectMapper = () => {
    return {description: ''};
}

export const getManageDataMapper = () => {
    return {description: ''};
}

export const getDataMapper = () => {
    return {description: ''};
}

export const getTemplateMapper = () => {
    return {description: '', projectsIds: []};
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
    return {description: '', configuration: []};
}

export const getColumnsMapper = () => {
    return {description: '', configuration: [], classId: 'none'};
}

export const getFiltersMapper = () => {
    return {description: '', configuration: [], classId: 'none'};
}

export const getTasksMapper = () => {
    return {description: '', classId: 'none'};
}

export const getEventsMapper = () => {
    return {description: ''};
}

export const getViewsMapper = () => {
    return {description: '', roleId: 0, featuresIds: []};
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