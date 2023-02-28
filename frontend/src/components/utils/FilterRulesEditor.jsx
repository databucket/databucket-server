import PropTypes from "prop-types";
import React, {useContext, useEffect, useState} from 'react';
import {Query, Utils as QbUtils} from '@react-awesome-query-builder/mui';
import EnumsContext from "../../context/enums/EnumsContext";
import PropertiesTable, {mergeProperties} from "./PropertiesTable";
import {createConfig, getInitialTree, renderBuilder, renderResult} from "./QueryBuilderHelper";
import '@react-awesome-query-builder/mui/css/styles.css';
import {Box} from "@mui/material";
import {useTheme} from "@mui/material/styles";

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

    const theme = useTheme();
    const enumsContext = useContext(EnumsContext);
    const {enums, fetchEnums} = enumsContext;
    const [properties, setFields] = useState(mergeProperties(props.configuration.properties, props.dataClass));
    const [state, setState] = useState({config: {}, tree: {}});

    useEffect(() => {
        const conf = createConfig(properties, props.tags, props.users, enums, theme);
        if (Object.keys(state.tree).length === 0) {
            const initialTree = QbUtils.checkTree(getInitialTree(props.configuration.logic, props.configuration.tree, conf), conf);
            setState({config: conf, tree: initialTree});
        } else {
            setState({config: conf, tree: state.tree});
        }
    }, [properties]);

    useEffect(() => {
        if (enums == null)
            fetchEnums();
    }, [enums, fetchEnums]);

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
        <>
            {props.activeTab === 0 && Object.keys(state.tree).length > 0 &&
                <Box sx={{margin: 0}}>
                    <Query
                        {...state.config}
                        value={state.tree}
                        onChange={onChange}
                        renderBuilder={renderBuilder}
                    />
                    {renderResult({tree: state.tree, config: state.config})}
                </Box>}
            {props.activeTab === 0 && <div/>}
            {props.activeTab === 1 &&
                <PropertiesTable
                    data={properties}
                    enums={enums}
                    onChange={handleChangeFields}
                    parentContentRef={props.parentContentRef}
                />}
        </>
    );
};
