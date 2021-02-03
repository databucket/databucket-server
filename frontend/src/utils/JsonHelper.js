export const isItemChanged = (data1, data2, keys) => {
    for (const key of keys) {
        const value1 = data1[key];
        const value2 = data2[key];
        if (Array.isArray(value1) && Array.isArray(value2)) {
            if (JSON.stringify(value1.sort()) !== JSON.stringify(value2.sort())) {
                return true;
            }
        } else if (value1 !== value2) {
            return true;
        }
    }

    return false;
}

export const getSelectedValues = (data, keys) => {
    let result = {};
    for (let key of keys)
        result[key] = data[key];

    return result;
}

export const validateItem = (data, specification) => {
    // exampleSpecification = {
    //     name: {title: 'Name', check: ['notEmpty', 'min3', 'max5']},
    //     description: {title: 'Description', check: ['max5']}
    // };
    let message = '';
    for (let key in specification) {
        if (specification.hasOwnProperty(key)) {
            let spec = specification[key];
            let title = spec['title'];
            let check = spec['check'];
            for (const validation of check) {
                if (validation === 'notEmpty' && (!data.hasOwnProperty(key) || data[key] == null || data[key].length === 0))
                    message += `${title} can not be empty!`;

                if (data.hasOwnProperty(key) && data[key] != null) {
                    if (validation.includes('min')) {
                        let min = parseInt(validation.substring(3));
                        if (data[key].length < min)
                            message += `${title} must be at least ${min} characters long!`;
                    }

                    if (validation.includes('max')) {
                        let max = parseInt(validation.substring(3));
                        if (data[key].length > max)
                            message += `${title} can be up to ${max} characters long!`;
                    }
                }
            }
        }
    }

    if (message.length > 0)
        return message;
    else return null;
}

/*
    Resolves problem with MaterialTable warning:
    Warning: `value` prop on `input` should not be null. Consider using an empty string to clear the component or `undefined` for uncontrolled components.
    Example:
    resolve({
        data: convertNullValues(result.data, ['description']),
        page: result.page,
        totalCount: result.total,
    })
 */
export const convertNullValues = (inputData, keys) => {
    for (let item of inputData) {
        for (let key of keys) {
            if (item[key] == null)
                item[key] = '';
        }
    }
    return inputData;
}

export const getProjectsIdsStr = (projectsIds) => {
    if (projectsIds != null) {
        const length = projectsIds.length;
        if (length > 4)
            return `${projectsIds.splice(0, 3).join(', ')}...[${length}]`;
        else
            return projectsIds.join(`, `);
    } else
        return '';
}

export const setSelectionProjects = (inputProjects, projectsIds) => {
    let projects = JSON.parse(JSON.stringify(inputProjects));

    if (projectsIds != null && projectsIds.length > 0)
        for (let project of projects)
            if (projectsIds.indexOf(project.id) > -1) {
                project['tableData'] = {};
                project['tableData']['checked'] = true;
            }

    return projects;
}

export const getRolesNames = (roles, rolesIds) => {
    if (rolesIds != null && rolesIds.length > 0 && roles.length > 0) {
        let rolesStr = '';
        for (let roleId of rolesIds) {
            let filteredRoles = roles.filter(r => r.id === roleId);
            if (filteredRoles.length > 0)
                rolesStr += ` ${filteredRoles[0].name.substring(0,1)}`;
            else
                rolesStr += " ?";
        }
        return rolesStr;
    } else
        return '';
}

export const sortByKey = (array, key) => {
    return array.sort(function (a, b) {
        const x = a[key];
        const y = b[key];
        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
    });
}