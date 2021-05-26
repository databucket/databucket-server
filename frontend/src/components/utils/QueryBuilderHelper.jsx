import InitialConfig from 'react-awesome-query-builder/lib/config/material';
import {Builder, Utils as QbUtils} from "react-awesome-query-builder";
import Typography from "@material-ui/core/Typography";
import React from "react";

export const getInitialTree = (loadedInitLogic, tree, config) => {
    if (tree && Object.keys(tree).length > 0) {
        return QbUtils.loadTree(tree);
    } else if (loadedInitLogic && Object.keys(loadedInitLogic).length > 0) {
        return QbUtils.loadFromJsonLogic(loadedInitLogic, config);
    } else {
        return QbUtils.loadTree({id: QbUtils.uuid(), type: "group"});
    }
}

export const renderResult = ({tree, config}) => {
    const pureSql = JSON.stringify(QbUtils.sqlFormat(tree, config));
    if (pureSql != null) {
        const sql = pureSql.substring(1, pureSql.length - 1).replaceAll("prop.", "").replaceAll("*", ".");
        return (
            <div style={{margin: '10px'}}>
                <Typography>{sql}</Typography>
            </div>
        );
    } else return (<div/>);
};

export const renderBuilder = (props) => (
    <div className="query-builder-container" style={{padding: '10px'}}>
        <div className="query-builder qb-lite">
            <Builder {...props} />
        </div>
    </div>
);

export const createConfig = (propFields, tags, users, enums) => {
    let fields = {};

    if (propFields != null && propFields.length > 0)
        fields['prop'] = buildPropertiesFields(propFields, enums);

    const userList = reduceUsersToList(users);
    const tagList = reduceTagsToList(tags);

    fields['id'] = id;
    if (tagList.length > 0)
        fields['tagId'] = tagId(tagList);
    fields['reserved'] = reserved;
    fields['owner'] = owner(userList);
    properties['properties'] = properties;
    fields['createdBy'] = createdBy(userList);
    fields['createdAt'] = createdAt;
    fields['modifiedBy'] = modifiedBy(userList);
    fields['modifiedAt'] = modifiedAt;

    return {...InitialConfig, fields: fields};
}

const reduceUsersToList = (fullUserList) => {
    return fullUserList.map(({username}) => ({value: username, title: username}));
}

const reduceTagsToList = (fullTagList) => {
    return fullTagList.map(({id, name}) => ({value: id, title: name}));
}

const buildPropertiesFields = (propFields, enums) => {
    let subfields = {}
    propFields.forEach(field => {
        let subfield = {
            label: field.title,
            type: field.type.replace('string', 'text').replace('numeric', 'number')
        };

        if (subfield.type === 'select') {
            const list = enums.find(e => e.id === field.enumId).items.map(item => ({value: item.value, title: item.text}));
            subfield['fieldSettings'] = {listValues: list};
            subfield['valueSources'] = ["value"];
        }

        if (subfield.type === 'text')
            subfield['excludeOperators'] = ["proximity", "starts_with", "ends_with"];

        if (subfield.type === 'number')
            subfield['excludeOperators'] = ["not_between", "starts_with", "ends_with"];

        subfields[field.path.replace(".", "*")] = subfield;
    });

    return {
        label: "Properties",
        type: "!struct",
        subfields: subfields
    };
}

const id = {
    label: 'Id',
    type: 'number',
    fieldSettings: {min: 0},
    preferWidgets: ['number'],
    valueSources: ["value"],
    excludeOperators: ["is_empty", "is_not_empty", "not_between"]
};

const tagId = (tagList) => {
    return {
        label: 'Tag',
        type: 'select',
        fieldSettings: {
            listValues: tagList
        },
        valueSources: ["value"]
    }
};

const reserved = {
    label: 'Reserved',
    type: 'boolean'
}

const owner = (userList) => {
    return {
        label: 'Owner',
        type: 'select',
        fieldSettings: {
            listValues: userList
        },
        valueSources: ["value"]
    }
}

const properties = {
    label: 'Properties',
    type: 'text',
    excludeOperators: ["proximity"]
}

const createdAt = {
    label: 'Created at',
    type: 'datetime',
    excludeOperators: ["is_empty", "is_not_empty", "not_between"]
}

const createdBy = (userList) => {
    return {
        label: 'Created by',
        type: 'select',
        fieldSettings: {
            listValues: userList
        },
        valueSources: ["value"]
    }
}

const modifiedAt = {
    label: 'Modified at',
    type: 'datetime',
    excludeOperators: ["is_empty", "is_not_empty", "not_between"]
}

const modifiedBy = (userList) => {
    return {
        label: 'Modified by',
        type: 'select',
        fieldSettings: {
            listValues: userList
        },
        valueSources: ["value"]
    }
}