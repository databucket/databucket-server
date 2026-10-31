import React from "react";
import {createEnumLookup, createTagLookup, createUserLookup} from "../../utils/JsonHelper";
import LookupIconDialog from "./EditLookupIconDialog";
import TableDynamicIcon from "../utils/TableDynamicIcon";

// sort: {colId, ord} and filters: {field: value} restore the saved state of the table,
// layout: {order: [field], hidden: {field: hidden}} the columns arranged by the user
export default function prepareTableColumns(columns, tags, enums, users, sort, filters, layout) {
    let colId = -1;
    if (columns != null) {
        const tableColumns = columns.configuration.columns.filter(col => col.enabled).map(col => {
            colId += 1;
            const ord = sort != null && colId === sort.colId ? sort.ord : null;
            let column = prepareColumn(col, columns.configuration.properties, tags, enums, users, ord);
            const filter = filters != null ? filters[column.field] : undefined;
            if (filter !== undefined)
                column = {...column, defaultFilter: filter};
            if (layout != null && layout.hidden != null && layout.hidden[column.field] !== undefined)
                column = {...column, hidden: layout.hidden[column.field]};
            return column;
        });
        return layout != null && layout.order != null ? arrangeColumns(tableColumns, layout.order) : tableColumns;
    } else
        return [];
}

// The table shows columns by tableData.columnOrder; the array keeps the order of the view, so column ids
// (and the saved sort) don't change. Columns added to the view since the order was saved go at the end.
const arrangeColumns = (tableColumns, order) => {
    const position = (column, index) => {
        const savedIndex = order.indexOf(column.field);
        return savedIndex >= 0 ? savedIndex : order.length + index;
    }
    const arranged = tableColumns.map((column, index) => ({column, index, position: position(column, index)}))
        .sort((a, b) => a.position - b.position);
    return tableColumns.map((column, index) => ({
        ...column,
        tableData: {columnOrder: arranged.findIndex(a => a.index === index)}
    }));
}

// the columns as the user arranged them in the table (dataManager.columns)
export const getColumnsLayout = (tableColumns) => {
    const hidden = {};
    tableColumns.forEach(column => hidden[column.field] = column.hidden === true);
    return {
        order: [...tableColumns].sort((a, b) => a.tableData.columnOrder - b.tableData.columnOrder).map(column => column.field),
        hidden: hidden
    };
}

const prepareColumn = (column, properties, tags, enums, users, ord) => {
    const userLookup = createUserLookup(users);

    let colDef = {
        editable: column.editable,
        filtering: column.filtering,
        hidden: column.hidden != null ? column.hidden : false,
        // no width defined: as wide as the content (an empty width made the table give each cell its full width,
        // spreading the spare space over all columns); the last column takes the rest, see BucketDataTable.css
        width: column.width != null && column.width !== "" ? column.width : "1px"
    };

    if (ord != null)
        colDef = {
            ...colDef,
            defaultSort: ord
        };

    switch (column.uuid) {
        case "uuid_data_id":
            return {
                title: "Id",
                field: 'Id',
                source: 'id',
                type: 'numeric',
                ...colDef
            };
        case "uuid_tag_id":
            const tagLookup = createTagLookup(tags);
            return {
                title: "Tag",
                field: 'Tag',
                source: 'tagId',
                type: 'numeric',
                lookup: tagLookup,
                ...colDef
            };
        case "uuid_reserved":
            return {
                title: "Reserved",
                field: 'Reserved',
                source: 'reserved',
                type: 'boolean',
                ...colDef
            };
        case "uuid_owner":
            return {
                title: "Owner",
                field: 'Owner',
                source: 'owner',
                type: 'string',
                lookup: userLookup,
                ...colDef
            };
        case "uuid_created_at":
            return {
                title: "Created at",
                field: 'Created at',
                source: 'createdAt',
                type: 'datetime',
                ...colDef
            };
        case "uuid_created_by":
            return {
                title: "Created by",
                field: 'Created by',
                source: 'createdBy',
                type: 'string',
                lookup: userLookup,
                ...colDef
            };
        case "uuid_modified_at":
            return {
                title: "Modified at",
                field: 'Modified at',
                source: 'modifiedAt',
                type: 'datetime',
                ...colDef
            };
        case "uuid_modified_by":
            return {
                title: "Modified by",
                field: 'Modified by',
                source: 'modifiedBy',
                type: 'string',
                lookup: userLookup,
                ...colDef
            };
        default:
            const property = properties.filter(prop => prop.uuid === column.uuid)[0];
            if (property.enumId != null && property.enumId > 0) {
                const enumObj = enums.filter(en => en.id === property.enumId)[0];
                const propLookup = createEnumLookup(enumObj);

                if (enumObj.iconsEnabled) {
                    return {
                        title: property.title,
                        field: property.title,
                        source: property.path,
                        type: 'string',
                        lookup: propLookup,
                        ...colDef,
                        render: rowData => <TableDynamicIcon icon={getIcon(enumObj.items, rowData[property.title])}/>,
                        editComponent: props =>
                            <LookupIconDialog
                                icon={getIcon(enumObj.items, props.rowData[property.title])}
                                items={enumObj.items}
                                onChange={props.onChange}
                            />
                    };
                } else {
                    return {
                        title: property.title,
                        field: property.title,
                        source: property.path,
                        type: 'string',
                        lookup: propLookup,
                        ...colDef
                    };
                }
            } else
                return {
                    title: property.title,
                    field: property.title,
                    source: property.path,
                    type: property.type,
                    ...colDef
                };
    }
}

const getIcon = (items, value) => {
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

export const getBucketFilters = (activeBucket, filters) => {
    return filters.filter(filterItem => (filterItem.classId != null && filterItem.classId === activeBucket.classId) || filterItem.classId == null);
}

export const sleep = (ms) => {
    return function(x) {
        return new Promise(resolve => setTimeout(() => resolve(x), ms));
    };
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
                                value = newItem.toISOString();

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
