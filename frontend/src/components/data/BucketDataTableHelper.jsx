import React from "react";
import {createEnumLookup, createTagLookup} from "../../utils/JsonHelper";
import LookupIconDialog from "./EditLookupIconDialog";
import TableDynamicIcon from "../utils/TableDynamicIcon";

export default function prepareTableColumns(columns, tags, enums) {
    if (columns != null)
        return columns.configuration.columns.filter(col => col.enabled).map(col => {
            return prepareColumn(col, columns.configuration.properties, tags, enums);
        });
    else
        return [];
}

const prepareColumn = (column, properties, tags, enums) => {
    switch (column.uuid) {
        case "uuid_data_id":
            return {
                title: "Id",
                field: 'Id',
                source: 'id',
                type: 'numeric',
                editable: column.editable,
                filtering: column.filtering,
                hidden: column.hidden
            };
        case "uuid_tag_id":
            const tagLookup = createTagLookup(tags);
            return {
                title: "Tag",
                field: 'Tag',
                source: 'tagId',
                type: 'numeric',
                editable: column.editable,
                filtering: column.filtering,
                hidden: column.hidden,
                lookup: tagLookup
            };
        case "uuid_reserved":
            return {
                title: "Reserved",
                field: 'Reserved',
                source: 'reserved',
                type: 'boolean',
                editable: column.editable,
                filtering: column.filtering,
                hidden: column.hidden
            };
        case "uuid_owner":
            return {
                title: "Owner",
                field: 'Owner',
                source: 'owner',
                type: 'string',
                editable: column.editable,
                filtering: column.filtering,
                hidden: column.hidden
            };
        case "uuid_created_at":
            return {
                title: "Created at",
                field: 'Created at',
                source: 'createdAt',
                type: 'datetime',
                editable: column.editable,
                filtering: column.filtering,
                hidden: column.hidden
            };
        case "uuid_created_by":
            return {
                title: "Created by",
                field: 'Created by',
                source: 'createdBy',
                type: 'string',
                editable: column.editable,
                filtering: column.filtering,
                hidden: column.hidden
            };
        case "uuid_modified_at":
            return {
                title: "Modified at",
                field: 'Modified at',
                source: 'modifiedAt',
                type: 'datetime',
                editable: column.editable,
                filtering: column.filtering,
                hidden: column.hidden
            };
        case "uuid_modified_by":
            return {
                title: "Modified by",
                field: 'Modified by',
                source: 'modifiedBy',
                type: 'string',
                editable: column.editable,
                filtering: column.filtering,
                hidden: column.hidden
            };
        default:
            const property = properties.filter(prop => prop.uuid === column.uuid)[0];
            if (property.enumId != null && property.enumId > 0) {
                const enumObj = enums.filter(en => en.id === property.enumId)[0];

                if (enumObj.iconsEnabled) {
                    return {
                        title: property.title,
                        field: property.title,
                        source: property.path,
                        type: 'string',
                        editable: column.editable,
                        filtering: column.filtering,
                        hidden: column.hidden,
                        render: rowData => <TableDynamicIcon iconName={getIconName(enumObj.items, rowData[property.title])}/>,
                        editComponent: props =>
                            <LookupIconDialog
                                selectedIconName={getIconName(enumObj.items, props.rowData[property.title])}
                                items={enumObj.items}
                                onChange={props.onChange}
                            />
                    };
                } else {
                    const propLookup = createEnumLookup(enumObj);
                    return {
                        title: property.title,
                        field: property.title,
                        source: property.path,
                        type: 'string',
                        editable: column.editable,
                        filtering: column.filtering,
                        hidden: column.hidden,
                        lookup: propLookup
                    };
                }
            } else
                return {
                    title: property.title,
                    field: property.title,
                    source: property.path,
                    type: property.type,
                    editable: column.editable,
                    filtering: column.filtering,
                    hidden: column.hidden
                };
    }
}

const getIconName = (items, value) => {
    const filteredItems = items.filter(item => (item.value === value));
    if (filteredItems.length > 0)
        return filteredItems[0].icon;
    else
        return null;
}

export const getBucketTags = (activeBucket, tags) => {
    return tags.filter(tag => (tag.bucketsIds != null && tag.bucketsIds.includes(activeBucket.id)) || (tag.classesIds != null && tag.classesIds.includes(activeBucket.classId)));
}

export const getBucketTasks = (activeBucket, tasks) => {
    return tasks.filter(task => (task.bucketsIds != null && task.bucketsIds.includes(activeBucket.id)) || (task.classesIds != null && task.classesIds.includes(activeBucket.classId)));
}

export const getBucketViews = (activeBucket, views) => {
    if (views != null && activeBucket != null) {
        return views.filter(view => (
            (view.classesIds != null && view.classesIds.includes(activeBucket.classId))
            ||
            (view.bucketsIds != null && view.bucketsIds.includes(activeBucket.id)))
        ).sort((a, b) => {
            return a.name > b.name ? 1 : -1
        });
    } else
        return [];
};

export const getActiveView = (bucketViews, lastActiveViewId) => {
    if (bucketViews != null && bucketViews.length > 0) {
        if (lastActiveViewId != null && bucketViews.find(view => view.id === lastActiveViewId)) {
            return bucketViews.filter(view => view.id === lastActiveViewId)[0];
        } else
            return bucketViews[0];
    } else
        return null;
}

export const convertDataBeforeAdd = (columns, inputDataRow) => {
    let payload = {properties: {}};
    try {
        for (let key in inputDataRow) {
            if (inputDataRow.hasOwnProperty(key)) {
                const source = getColumnSource(columns, key);
                const fieldType = getFieldType(columns, key);
                if (fieldType === 'numeric') {
                    payload[source] = parseInt(inputDataRow[key], 10);
                } else if (fieldType === 'datetime' || fieldType === 'date' || fieldType === 'time') {
                    payload[source] = toIsoString(inputDataRow[key]);
                } else {
                    payload[source] = inputDataRow[key];
                }

                if (source.startsWith('$')) {
                    const path = source.substring(2);
                    setJsonValueByPath(path, inputDataRow[key], payload.properties);
                    delete payload[source];
                }
            }
        }
    } catch (error) {
        console.error(error);
    }

    return payload;
}

export const convertDataBeforeModify = (columns, newData, oldData) => {
    const readOnlyColumns = ['id', 'owner', 'createdAt', 'createdBy', 'modifiedAt', 'modifiedBy'];

    let payload = {propertiesToSet: {}};
    let hasUpdateProperties = false;

    try {
        for (let key in newData) {
            if (newData.hasOwnProperty(key)) {
                let source = getColumnSource(columns, key);
                if (readOnlyColumns.indexOf(source) < 0) {
                    const newItem = newData[key];
                    const oldItem = oldData[key];

                    if (newItem !== oldItem) {
                        const fieldType = getFieldType(columns, key);
                        if (newItem != null) {
                            let value = newItem;
                            if (fieldType === 'numeric')
                                value = parseInt(newItem, 10);
                            else if (fieldType === 'datetime' || fieldType === 'date' || fieldType === 'time')
                                value = toIsoString(newItem);

                            if (source.startsWith('$')) {
                                payload.propertiesToSet[source] = value;
                                hasUpdateProperties = true;
                            } else
                                payload[source] = value;
                        } else {
                            if (source.startsWith('$')) {
                                payload.propertiesToSet[source] = null;
                                hasUpdateProperties = true;
                            } else
                                payload[source] = null;
                        }
                    }
                }
            }
        }
    } catch (error) {
        console.error(error);
    }
    if (!hasUpdateProperties)
        delete payload['propertiesToSet'];

    return payload;
}

export const getColumnSource = (columns, fieldName) => {
    let c = columns.filter(c => (c.field === fieldName))[0];
    return c.source;
}

const getFieldType = (columns, fieldName) => {
    const c = columns.filter(column => (column.field === fieldName))[0];
    return c.type;
}

export const getDataRowId = (columns, data) => {
    return data.Id;
    // let dataIdColumn = this.state.columns.filter(c => (c.source === 'data_id'))[0];
    // return data[dataIdColumn.field];
}

const setJsonValueByPath = (path, val, obj) => {
    let fields = path.split('.');
    let result = obj;
    for (let i = 0, n = fields.length; i < n && result !== undefined; i++) {
        let field = fields[i];
        if (i === n - 1) {
            result[field] = val;
        } else {
            if (typeof result[field] === 'undefined') {
                result[field] = {};
            }
            result = result[field];
        }
    }
}

const toIsoString = (aDate) => {
    let tzo = -aDate.getTimezoneOffset(),
        dif = tzo >= 0 ? '+' : '-',
        pad = function (num) {
            let norm = Math.floor(Math.abs(num));
            return (norm < 10 ? '0' : '') + norm;
        };
    return aDate.getFullYear() +
        '-' + pad(aDate.getMonth() + 1) +
        '-' + pad(aDate.getDate()) +
        'T' + pad(aDate.getHours()) +
        ':' + pad(aDate.getMinutes()) +
        ':' + pad(aDate.getSeconds()) +
        ".000" +
        dif + pad(tzo / 60) + pad(tzo % 60);
}

export const getFetchColumns = (tableColumns) => {
    return tableColumns.map(col => ({field: col.source, title: col.title}));
}
