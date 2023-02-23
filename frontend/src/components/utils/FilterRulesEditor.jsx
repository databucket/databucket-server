import PropTypes from "prop-types";
import React, {useContext, useEffect, useState} from 'react';
import {Query, Builder, Utils as QbUtils} from '@react-awesome-query-builder/ui';
import EnumsContext from "../../context/enums/EnumsContext";
import Typography from "@mui/material/Typography";
import PropertiesTable, {mergeProperties} from "./PropertiesTable";
import {createConfig, getInitialTree} from "./QueryBuilderHelper";

FilterRulesEditor.propTypes = {
    activeTab: PropTypes.number.isRequired,
    configuration: PropTypes.object,
    dataClass: PropTypes.object,
    tags: PropTypes.array.isRequired,
    users: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    parentContentRef: PropTypes.object.isRequired
}

export default function FilterRulesEditor(props) {

    const enumsContext = useContext(EnumsContext);
    const {enums, fetchEnums} = enumsContext;
    const [properties, setFields] = useState(mergeProperties(props.configuration.properties, props.dataClass));
    const [state, setState] = useState({config: {}, tree: {}});

    useEffect(() => {
        const conf = createConfig(properties, props.tags, props.users, enums);
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
            const sql = pureSql.substring(1, pureSql.length - 1).replaceAll("prop.", "").replaceAll("*", ".");
            return (
                <div style={{margin: '10px'}}>
                    <Typography>{sql}</Typography>
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
            {props.activeTab === 1 &&
            <PropertiesTable
                data={properties}
                enums={enums}
                onChange={handleChangeFields}
                parentContentRef={props.parentContentRef}
            />}
        </div>
    );
};
