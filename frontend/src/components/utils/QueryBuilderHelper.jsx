import {
    Builder,
    MuiConfig,
    Utils as QbUtils
} from '@react-awesome-query-builder/mui';
import React, {useState} from "react";
import {Box, IconButton, styled, TextField, Tooltip, Typography} from "@mui/material";
import {Event as CalendarIcon, Update as RelativeIcon} from "@mui/icons-material";
import './../styles/query-builder-styles.css';
import {isRelative, isValidRelative, withRelativeDateFormats} from "../../utils/RelativeDate";

/**
 * The date or datetime widget of the query builder with a switch to a relative date (@now-2h): a filter like "modified
 * more than 2 hours ago" then stays valid instead of holding a fixed date.
 */
const RelativeDateWidget = ({widgetProps, base, ctx}) => {
    const {value, setValue, readonly} = widgetProps;
    const [relative, setRelative] = useState(isRelative(value));

    const toggle = () => {
        setValue(relative ? undefined : '@now-1d');
        setRelative(!relative);
    };

    const invalid = relative && value != null && value !== '' && !isValidRelative(value);

    return (
        <Box sx={{display: 'flex', alignItems: 'center'}}>
            {relative
                ? <TextField variant='standard' size='small' value={value || ''} disabled={readonly}
                             placeholder='@now-2h' error={invalid} sx={{width: 120}}
                             inputProps={{style: {fontFamily: 'monospace'}}}
                             title='@now, @now-2h, @now+1d (s, m, h, d, w)'
                             onChange={e => setValue(e.target.value)}/>
                : base.factory(widgetProps, ctx)}
            {!readonly &&
                <Tooltip title={relative ? 'Pick a date' : 'Relative to now, e.g. @now-2h'}>
                    <IconButton size='small' onClick={toggle} aria-label={relative ? 'Pick a date' : 'Relative date'}>
                        {relative ? <CalendarIcon fontSize='small'/> : <RelativeIcon fontSize='small'/>}
                    </IconButton>
                </Tooltip>}
        </Box>
    );
};

// the date widget of the config with a relative date allowed (formats, export, import: RelativeDate.js)
const withRelativeDate = base => ({
    ...withRelativeDateFormats(base),
    factory: (props, ctx) => <RelativeDateWidget widgetProps={props} base={base} ctx={ctx}/>
});

export const getInitialTree = (loadedInitLogic, tree, config) => {
    if (tree && Object.keys(tree).length > 0) {
        return QbUtils.loadTree(tree);
    } else if (loadedInitLogic && Object.keys(loadedInitLogic).length > 0) {
        return QbUtils.loadFromJsonLogic(loadedInitLogic, config);
    } else {
        return QbUtils.loadTree({id: QbUtils.uuid(), type: "group"});
    }
}

const StyledDiv = styled('div')(({theme}) => ({
    margin: theme.spacing(2)
}));

export const renderResult = ({tree, config}) => {
    const pureSql = JSON.stringify(QbUtils.sqlFormat(tree, config));
    if (pureSql != null) {
        const sql = pureSql.substring(1, pureSql.length - 1).replaceAll("prop.", "").replaceAll("*", ".");
        return (
            <StyledDiv>
                <Typography>{sql}</Typography>
            </StyledDiv>
        );
    } else return (<div/>);
};

const StyledBuilder = styled('div')(({theme}) => ({
    "& .rule": {
        backgroundColor: theme.palette.background.paper
    },
    //
    // "& .group": {
    //     backgroundColor: alpha(theme.palette.text.primary, 0.5)
    // },
}));


export const renderBuilder = (props) => (
    <StyledBuilder className="query-builder-container">
        <div className="query-builder qb-lite">
            <Builder {...props} />
        </div>
    </StyledBuilder>
);

export const createConfig = (propFields, tags, users, enums, currentTheme) => {
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

    return {
        ...MuiConfig,
        widgets: {
            ...MuiConfig.widgets,
            date: withRelativeDate(MuiConfig.widgets.date),
            datetime: withRelativeDate(MuiConfig.widgets.datetime)
        },
        fields: fields,
        settings: {
            ...MuiConfig.settings,
            theme: {mui: currentTheme}
        }
    };
}

const reduceUsersToList = (fullUserList) => {
    return [...fullUserList.map(({username}) => ({value: username, title: username})), {
        value: '@currentUser',
        title: '@currentUser'
    }];
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
            const list = enums.find(e => e.id === field.enumId).items.map(item => ({
                value: item.value,
                title: item.text
            }));
            subfield['fieldSettings'] = {listValues: list};
        }

        if (subfield.type === 'text')
            subfield['excludeOperators'] = ["proximity", "starts_with", "ends_with"];

        if (subfield.type === 'boolean')
            subfield['operators'] = ["equal", "not_equal", "is_empty", "is_not_empty"];

        subfield['valueSources'] = ["value"];

        subfields[field.path.replaceAll(".", "*")] = subfield;
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
