export const getManageProjectMapper = () => {
    return {description: ''};
}

export const getManageUserMapper = () => {
    return {rolesIds: [], projectsIds: []};
}

export const getUserMapper = () => {
    return {rolesIds: [], groupsIds: [], bucketsIds: [], viewsIds: []};
}

export const getGroupMapper = () => {
    return {description: '', bucketsIds: []};
}

export const getBucketMapper = () => {
    return {description: '', groupsIds: [], usersIds: [], classId: 'none'};
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