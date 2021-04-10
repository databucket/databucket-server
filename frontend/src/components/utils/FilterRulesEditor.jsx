import PropTypes from "prop-types";
import React, {useContext, useEffect, useState} from 'react';
import './awesome-query-builder-styles.css';
import {Query, Builder, Utils as QbUtils} from 'react-awesome-query-builder';
import InitialConfig from 'react-awesome-query-builder/lib/config/material';
import EnumsContext from "../../context/enums/EnumsContext";
import Typography from "@material-ui/core/Typography";
import PropertiesTable, {mergeProperties} from "./PropertiesTable";

FilterRulesEditor.propTypes = {
    activeTab: PropTypes.number.isRequired,
    configuration: PropTypes.object,
    dataClass: PropTypes.object,
    tags: PropTypes.array.isRequired,
    users: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired
}

const getInitialTree = (loadedInitLogic, tree, config) => {
    if (tree && Object.keys(tree).length > 0) {
        return QbUtils.loadTree(tree);
    } else if (loadedInitLogic && Object.keys(loadedInitLogic).length > 0) {
        return QbUtils.loadFromJsonLogic(loadedInitLogic, config);
    } else {
        return QbUtils.loadTree({id: QbUtils.uuid(), type: "group"});
    }
}

export default function FilterRulesEditor(props) {

    const enumsContext = useContext(EnumsContext);
    const {enums, fetchEnums} = enumsContext;
    const [properties, setFields] = useState(mergeProperties(props.configuration.properties, props.dataClass));
    const [state, setState] = useState({config: {}, tree: {}});

    useEffect(() => {
        const conf = createConfig(properties, props.tags, props.users, props.dataClass, enums);
        if (Object.keys(state.tree).length === 0) {
            const initialTree = QbUtils.checkTree(getInitialTree(props.configuration.logic, props.configuration.tree, conf), conf);
            setState({config: conf, tree: initialTree});
        } else {
            setState({config: conf, tree: state.tree});
        }

        // eslint-disable-next-line
    }, [properties]);

    useEffect(() => {
        if (enums == null)
            fetchEnums();
    }, [enums, fetchEnums]);

    const renderBuilder = (props) => (
        <div className="query-builder-container" style={{padding: '10px'}}>
            <div className="query-builder qb-lite">
                <Builder {...props} />
            </div>
        </div>
    );

    const renderResult = ({tree, config}) => {
        const pureSql = JSON.stringify(QbUtils.sqlFormat(tree, config));
        if (pureSql != null) {
            const sql = pureSql.substring(1, pureSql.length - 1);
            return (
                <div style={{margin: '10px'}}>
                    <Typography>{sql}</Typography>
                    {/*<div>JsonLogic: <pre>{JSON.stringify(QbUtils.jsonLogicFormat(tree, config).logic)}</pre></div>*/}
                    {/*<div>Json: <pre>{JSON.stringify(tree)}</pre></div>*/}
                </div>
            );
        } else return (<div/>);
    };

    const onChange = (tree, config) => {
        setState({config, tree});
        props.onChange({properties, config, tree});
    };

    const handleChangeFields = (properties) => {
        const config = state.config;
        const tree = state.tree;
        setFields(properties);
        props.onChange({properties, config, tree});
    }

    return (
        <div>
            {props.activeTab === 0 && Object.keys(state.tree).length > 0 &&
            <div>
                <Query
                    {...state.config}
                    value={state.tree}
                    onChange={onChange}
                    renderBuilder={renderBuilder}
                />
                {renderResult({tree: state.tree, config: state.config})}
            </div>}
            {props.activeTab === 0 && <div/>}
            {props.activeTab === 1 && <PropertiesTable data={properties} onChange={handleChangeFields}/>}
        </div>
    );
};

const createConfig = (propFields, tags, users, dataClass, enums) => {
    let fields = {};

    if (propFields != null && propFields.length > 0)
        fields['prop'] = buildPropertiesFields(propFields, dataClass, enums);

    const userList = reduceUsersToList(users);
    const tagList = reduceTagsToList(tags);

    fields['dataId'] = dataId;
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
};

const reduceUsersToList = (fullUserList) => {
    return fullUserList.map(({username}) => ({value: username, title: username}));
}

const reduceTagsToList = (fullTagList) => {
    return fullTagList.map(({id, name}) => ({value: id, title: name}));
}

const buildPropertiesFields = (propFields, dataClass, enums) => {
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
            subfield['excludeOperators'] = ["proximity"];

        subfields[field.path.replace(".", "*")] = subfield;
    });

    return {
        label: "Properties",
        type: "!struct",
        subfields: subfields
    };
}

const dataId = {
    label: 'Id',
    type: 'number',
    fieldSettings: {min: 0},
    preferWidgets: ['number']
};

const tagId = (tagList) => {
    return {
        label: 'Tag',
        type: 'select',
        fieldSettings: {
            listValues: tagList
        },
        // valueSources: ["value"]
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
        // valueSources: ["value"]
    }
}

const properties = {
    label: 'Properties',
    type: 'text',
    excludeOperators: ["proximity"]
}

const createdAt = {
    label: 'Created at',
    type: 'datetime'
}

const createdBy = (userList) => {
    return {
        label: 'Created by',
        type: 'select',
        fieldSettings: {
            listValues: userList
        },
        // valueSources: ["value"]
    }
}

const modifiedAt = {
    label: 'Modified at',
    type: 'datetime'
}

const modifiedBy = (userList) => {
    return {
        label: 'Modified by',
        type: 'select',
        fieldSettings: {
            listValues: userList
        },
        // valueSources: ["value"]
    }
}