import PropTypes from "prop-types";
import React, {useEffect, useState} from 'react';
import {Query, Utils as QbUtils} from '@react-awesome-query-builder/mui';
import PropertiesTable, {mergeProperties} from "./PropertiesTable";
import {createConfig, getInitialTree, renderBuilder, renderResult} from "./QueryBuilderHelper";

FilterRulesEditorTemplate.propTypes = {
    activeTab: PropTypes.number.isRequired,
    configuration: PropTypes.object,
    dataClass: PropTypes.object,
    tags: PropTypes.array.isRequired,
    users: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    parentContentRef: PropTypes.object.isRequired,
    enums: PropTypes.array.isRequired
}

export default function FilterRulesEditorTemplate(props) {

    const [properties, setFields] = useState(mergeProperties(props.configuration.properties, props.dataClass));
    const [state, setState] = useState({config: {}, tree: {}});

    useEffect(() => {
        const conf = createConfig(properties, props.tags, props.users, props.enums);
        if (Object.keys(state.tree).length === 0) {
            const initialTree = QbUtils.checkTree(getInitialTree(props.configuration.logic, props.configuration.tree, conf), conf);
            setState({config: conf, tree: initialTree});
        } else {
            setState({config: conf, tree: state.tree});
        }

        // eslint-disable-next-line
    }, [properties]);

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
            {props.activeTab === 1 &&
                <PropertiesTable
                    data={properties}
                    enums={props.enums}
                    onChange={handleChangeFields}
                    parentContentRef={props.parentContentRef}
                />}
        </div>
    );
};
