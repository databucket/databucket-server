import React from "react";
import {createEnumLookup, createTagLookup} from "../../utils/JsonHelper";
import LookupIconDialog from "./EditLookupIconDialog";
import TableDynamicIcon from "../utils/TableDynamicIcon";

export default function prepareViewColumns(columns, tags, enums) {
    if (columns == null)
        return [];
    else
        return columns.configuration.columns.filter(col => col.enabled).map(col => {
            return prepareColumn(col, columns.configuration.properties, tags, enums);
        });
}

const getIconName = (items, value) => {
    // console.log('getIconName: (items, value)');
    // console.log(items);
    // console.log(value);
    const filteredItems = items.filter(item => (item.value === value));
    if (filteredItems.length > 0)
        return filteredItems[0].icon;
    else
        return null;
}

const prepareColumn = (column, properties, tags, enums) => {
    switch (column.uuid) {
        case "uuid_data_id":
            return {
                title: "Id",
                field: 'id',
                type: 'numeric',
                editable: column.editable,
                filtering: column.filtering,
                // align: column.align,
                hidden: column.hidden
            };
        case "uuid_tag_id":
            const tagLookup = createTagLookup(tags);
            return {
                title: "Tag",
                field: 'tagId',
                type: 'numeric',
                editable: column.editable,
                filtering: column.filtering,
                // align: column.align,
                hidden: column.hidden,
                lookup: tagLookup
            };
        case "uuid_reserved":
            return {
                title: "Reserved",
                field: 'reserved',
                type: 'boolean',
                editable: column.editable,
                filtering: column.filtering,
                // align: column.align,
                hidden: column.hidden
            };
        case "uuid_owner":
            return {
                title: "Owner",
                field: 'owner',
                type: 'string',
                editable: column.editable,
                filtering: column.filtering,
                // align: column.align,
                hidden: column.hidden
            };
        case "uuid_created_at":
            return {
                title: "Created at",
                field: 'createdAt',
                type: 'datetime',
                editable: column.editable,
                filtering: column.filtering,
                // align: column.align,
                hidden: column.hidden
            };
        case "uuid_created_by":
            return {
                title: "Created by",
                field: 'createdBy',
                type: 'string',
                editable: column.editable,
                filtering: column.filtering,
                // align: column.align,
                hidden: column.hidden
            };
        case "uuid_modified_at":
            return {
                title: "Modified at",
                field: 'modifiedAt',
                type: 'datetime',
                editable: column.editable,
                filtering: column.filtering,
                // align: column.align,
                hidden: column.hidden
            };
        case "uuid_modified_by":
            return {
                title: "Modified by",
                field: 'modifiedBy',
                type: 'string',
                editable: column.editable,
                filtering: column.filtering,
                // align: column.align,
                hidden: column.hidden
            };
        default:
            const property = properties.filter(prop => prop.uuid === column.uuid)[0];
            if (property.enumId != null && property.enumId > 0) {
                const enumObj = enums.filter(en => en.id === property.enumId)[0];

                if (enumObj.iconsEnabled) {
                    const colField = "prop_" + property.path.replace(".", "#");
                    return {
                        title: property.title,
                        field: colField,
                        type: 'numeric',
                        editable: column.editable,
                        filtering: column.filtering,
                        // align: column.align,
                        hidden: column.hidden,
                        render: rowData => <TableDynamicIcon iconName={getIconName(enumObj.items, rowData[colField])}/>,
                        editComponent: props =>
                            <LookupIconDialog
                                selectedIconName={getIconName(enumObj.items, props.rowData[column.field])}
                                items={enumObj.items}
                                onChange={props.onChange}
                            />
                    };
                } else {
                    const propLookup = createEnumLookup(enumObj);
                    return {
                        title: property.title,
                        field: "prop_" + property.path.replace(".", "#"),
                        type: property.type !== 'select' ? property.type : 'numeric',
                        editable: column.editable,
                        filtering: column.filtering,
                        // align: column.align,
                        hidden: column.hidden,
                        lookup: propLookup
                    };
                }
            } else
                return {
                    title: property.title,
                    field: "prop_" + property.path.replace(".", "#"),
                    type: property.type,
                    editable: column.editable,
                    filtering: column.filtering,
                    // align: column.align,
                    hidden: column.hidden
                };
    }
}

export const getBucketTags = (activeBucket, tags) => {
    return tags.filter(tag => (tag.bucketsIds != null && tag.bucketsIds.includes(activeBucket.id)) || (tag.classesIds != null && tag.classesIds.includes(activeBucket.classId)));
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
