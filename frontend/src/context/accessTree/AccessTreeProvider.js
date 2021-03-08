import React, {useReducer} from 'react';
import AccessTreeContext from "./AccessTreeContext";
import AccessTreeReducer from "./AccessTreeReducer";
import {getBaseUrl, getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";


const AccessTreeProvider = props => {

    const initialState = {
        accessTree: null,
        activeGroup: null,
        activeBucket: null,
        bucketsTabs: []
    }

    const [state, dispatch] = useReducer(AccessTreeReducer, initialState);

    const fetchAccessTree = () => {
        fetch(getBaseUrl('users/access-tree'), getGetOptions())
            .then(handleErrors)
            .then(accessTree => dispatch({
                type: "FETCH_ACCESS_TREE",
                payload: accessTree
            }))
            .catch(err => console.log(err));
    }

    const setActiveGroup = (group) => {
        dispatch({
            type: "SET_ACTIVE_GROUP",
            payload: group
        });
    };

    const setActiveBucket = (bucket) => {
        dispatch({
            type: "SET_ACTIVE_BUCKET",
            payload: bucket
        });
    };

    const addTab = (bucket) => {
        dispatch({
            type: "ADD_TAB",
            payload: bucket
        });
    };

    const removeTab = (bucket) => {
        dispatch({
            type: "REMOVE_TAB",
            payload: bucket
        });
    };

    return (
        <AccessTreeContext.Provider value={
            {
                accessTree: state.accessTree,
                activeGroup: state.activeGroup,
                activeBucket: state.activeBucket,
                bucketsTabs: state.bucketsTabs,
                fetchAccessTree,
                setActiveGroup,
                setActiveBucket,
                addTab,
                removeTab
            }
        }>
            {props.children}
        </AccessTreeContext.Provider>
    );
}

export default AccessTreeProvider;