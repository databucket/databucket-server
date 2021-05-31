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
                    message += `>>> ${title} can not be empty! `;

                if (data.hasOwnProperty(key) && data[key] != null && data[key].length > 0) {
                    if (validation.includes('min')) {
                        let min = parseInt(validation.substring(3), 10);
                        if (data[key].length < min)
                            message += `>>> ${title} must be at least ${min} characters long! `;
                    }

                    if (validation.includes('max')) {
                        let max = parseInt(validation.substring(3), 10);
                        if (data[key].length > max)
                            message += `>>> ${title} can be up to ${max} characters long! `;
                    }

                    if (validation === 'selected') {
                        let value = parseInt(data[key], 10);
                        if (value <= 0)
                            message += `>>> ${title} must be selected! `;
                    }

                    if (validation === 'custom-check-enum-items') {
                        const valueSet = new Set(data.items.map(({value}) => value));

                        if (valueSet.size !== data.items.length)
                            message += `>>> Enum values must be unique! `;

                        if (data.iconsEnabled === true && data.items.filter(item => item.icon == null).length > 0)
                            message += `>>> Every item must have defined an icon! `;
                    }

                    if (validation === 'validJsonPath') {
                        const subPaths = data[key].split(".");
                        if (subPaths[0] !== '$')
                            message += `>>> The path must start with a '$' character! `;

                        const myRegEx  = /[^a-zA-Z0-9_\\(\\)\\-]/i;
                        for (let i = 1; i < subPaths.length; i++) {
                            const valid = !(myRegEx.test(subPaths[i]));
                            if (!valid) {
                                message += `>>> The path can contain the following characters only '$.a-zA-Z0-9_-()'! `;
                                break;
                            }
                        }

                        if (data[key].includes(".."))
                            message += `>>> The path can contain double dots! `;
                    }

                    if (validation === 'validClassPropertyType') {
                        if (data[key] === 'select' && data['enumId'] == null)
                            message += `>>> Enum must not be empty for 'Enum' type! `;
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
        data: convertNullValuesInCollection(result.data, {description: '', bucketsIds: []}}),
        page: result.page,
        totalCount: result.total,
    })
 */
export const convertNullValuesInCollection = (inputCollection, mappers) => {
    inputCollection.forEach(function (item) {
        for (let key in mappers) {
            if (mappers.hasOwnProperty(key))
                if (item[key] == null)
                    item[key] = mappers[key];
        }
    });

    return inputCollection;
}

export const convertNullValuesInObject = (inputObject, mappers) => {
    for (let key in mappers) {
        if (mappers.hasOwnProperty(key))
            if (inputObject[key] == null)
                inputObject[key] = mappers[key];
    }
    return inputObject;
}

export const convertPropertiesDates = (actionProperties, defProperties) => {
    return actionProperties.map(property => {
        const propertyDef = defProperties.filter(propDef => propDef.uuid === property.uuid)[0];
        if (['date', 'datetime', 'time'].includes(propertyDef.type)) {
            return {...property, value: new Date(property.value)};
        } else
            return property;
    });
}

export const getIdsStr = (inputIdsArray) => {
    if (inputIdsArray != null && inputIdsArray.length > 0) {
        let ids = JSON.parse(JSON.stringify(inputIdsArray));
        const length = ids.length;
        if (length > 4)
            return `${ids.splice(0, 3).join(', ')}...[${length}]`;
        else
            return ids.join(`, `);
    } else
        return '';
}

export const getArrayLengthStr = (inputIdsArray) => {
    if (inputIdsArray != null && inputIdsArray.length > 0)
        return `${inputIdsArray.length}`;
    else
        return '0';
}

export const getObjectLengthStr = (inputObject) => {
    if (inputObject != null) {
        const items = JSON.stringify(inputObject).trim().match(/\[{"var":/g);
        if (items != null)
            return `${items.length}`;
    }
    return '0';
}

export const getItemName = (objectCollection, id) => {
    if (objectCollection != null && objectCollection.length > 0 && id != null && id > 0) {
        return objectCollection.find(item => item.id === id).name;
    } else
        return '';
}

export const setSelectionItemById = (inputItems, itemId) => {
    let items = JSON.parse(JSON.stringify(inputItems));

    if (itemId > 0)
        for (let item of items)
            if (item.id === itemId) {
                item['tableData'] = {};
                item['tableData']['checked'] = true;
            }

    return items;
}

export const setSelectionItemsByIds = (inputItems, itemsIds) => {
    let items = JSON.parse(JSON.stringify(inputItems));

    if (itemsIds != null && itemsIds.length > 0)
        for (let item of items)
            if (itemsIds.indexOf(item.id) > -1) {
                item['tableData'] = {};
                item['tableData']['checked'] = true;
            }

    return items;
}

export const getRoleName = (roles, roleId) => {
    if (roles != null && roleId != null && roleId > 0) {
        return roles.find(r => r.id === roleId).name;
    } else
        return '- none -';
}

export const getAdminMemberRolesLookup = (roles) => {
    let lookup = {0: ' - none -'}
    if (roles != null) {
        return roles.filter(role => ['ADMIN', 'MEMBER'].includes(role.name))
            .reduce((obj, item) => {
                return {
                    ...obj,
                    [item.id]: item.name,
                };
            }, lookup);
    } else
        return {};
}

export const createEnumLookup = (enumObj) => {
    return enumObj.items.reduce((obj, item) => {
        return {
            ...obj,
            [item.value]: item.text,
        };
    }, {});
}

export const createTagLookup = (tags) => {
    if (tags != null && tags.length > 0)
        return tags.reduce((obj, item) => {
            return {
                ...obj,
                [item.id]: item.name,
            };
        }, {});
    else
        return {};
}

export const getRolesNames = (roles, rolesIds) => {
    if (roles != null && rolesIds != null && rolesIds.length > 0 && roles.length > 0) {
        if (rolesIds.length === 1)
            return roles.find(r => r.id === rolesIds[0]).name;
        else {
            let rolesStr = '';
            for (let roleId of rolesIds) {
                let filteredRoles = roles.filter(r => r.id === roleId);
                if (filteredRoles.length > 0)
                    rolesStr += `${filteredRoles[0].name.substring(0, 1)}.`;
                else
                    rolesStr += " ?";
            }
            return rolesStr;
        }
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


export const notifierChangeAdapter = (items, payload) => {
    const fieldName = payload.itemsTargetFieldName;
    const sourceObjId = payload.sourceObjectId;
    const sourceObjItemsIds = payload.sourceObjectItemsIds;

    return items.map(item => {
        if (sourceObjItemsIds != null && sourceObjItemsIds.includes(item.id)) {
            // make sure this item is in the array
            if (item[fieldName] == null) {
                item[fieldName] = [];
                item[fieldName].push(sourceObjId);
            } else if (!item[fieldName].includes(sourceObjId))
                item[fieldName].push(sourceObjId);
        } else {
            // make sure this item is not in the array
            if (item[fieldName] != null && item[fieldName].includes(sourceObjId))
                item[fieldName] = item[fieldName].filter(id => id !== sourceObjId);
        }
        return item;
    });
}

export const arraysEquals = (newData, oldData, fieldName) => {
    const newArray = newData[fieldName];
    const oldArray = oldData[fieldName];

    if (newArray == null || oldArray == null)
        return newArray === oldArray;
    else
        return JSON.stringify(newArray.sort()) === JSON.stringify(oldArray.sort());
}

export const uuidV4 = () => {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        // eslint-disable-next-line
        const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

export const getClassById = (classes, id) => {
    const dataClass = (classes != null && id != null && id !== 'none') ? classes.filter(c => c.id === parseInt(id, 10)) : [];
    return dataClass.length > 0 ? dataClass[0] : null;
}

export const getPropertyByUuid = (properties, uuid) => {
    return properties.filter(property => property.uuid === uuid)[0];
}

export const getPropertyTitle = (properties, uuid) => {
    return getPropertyByUuid(properties, uuid).title;
}