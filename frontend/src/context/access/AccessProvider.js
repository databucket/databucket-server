import React, {useReducer} from 'react';
import AccessContext from "./AccessContext";
import AccessReducer from "./AccessReducer";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getViewsMapper} from "../../utils/NullValueMappers";
import {getBaseUrl, getBaseUrlWithIds} from "../../utils/UrlBuilder";


const AccessProvider = props => {

    const initialState = {
        accessTree: null,
        activeGroup: null,
        activeBucket: null,
        bucketsTabs: [],
        views: null,
        columns: null,
        filters: null,
        tasks: null
    }

    const [state, dispatch] = useReducer(AccessReducer, initialState);

    const fetchAccessTree = () => {
        fetch(getBaseUrl('users/access-tree'), getGetOptions())
            .then(handleErrors)
            .then(accessTree => dispatch({
                type: "FETCH_ACCESS_TREE",
                payload: accessTree
            }))
            .catch(err => console.log(err));
    }

    const fetchViews = () => {
        const ids = getViewsIds();
        if (ids.length > 0)
            fetch(getBaseUrlWithIds('users/views', ids), getGetOptions())
                .then(handleErrors)
                .then(views => dispatch({
                    type: "FETCH_VIEWS",
                    payload: convertNullValuesInCollection(views, getViewsMapper())
                }))
                .catch(err => console.log(err));
    }

    const getViewsIds = () => {
        return state.accessTree.views.map(({id}) => id);
    }

    const fetchColumns = () => {
        fetch(getBaseUrlWithIds('users/columns', getColumnsIds()), getGetOptions())
            .then(handleErrors)
            .then(columns => dispatch({
                type: "FETCH_COLUMNS",
                payload: columns
            }))
            .catch(err => console.log(err));
    }

    const getColumnsIds = () => {
        return [...new Set(state.views.map(({columnsId}) => columnsId))];
    }

    const fetchFilters = () => {
        fetch(getBaseUrl('users/filters'), getGetOptions())
            .then(handleErrors)
            .then(filters => dispatch({
                type: "FETCH_FILTERS",
                payload: filters
            }))
            .catch(err => console.log(err));
    }

    const fetchTasks = () => {
        fetch(getBaseUrl('users/tasks'), getGetOptions())
            .then(handleErrors)
            .then(tasks => dispatch({
                type: "FETCH_TASKS",
                payload: tasks
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
        <AccessContext.Provider value={
            {
                accessTree: state.accessTree,
                activeGroup: state.activeGroup,
                activeBucket: state.activeBucket,
                bucketsTabs: state.bucketsTabs,
                views: state.views,
                columns: state.columns,
                filters: state.filters,
                tasks: state.tasks,
                fetchAccessTree,
                setActiveGroup,
                setActiveBucket,
                addTab,
                removeTab,
                fetchViews,
                fetchColumns,
                fetchFilters,
                fetchTasks
            }
        }>
            {props.children}
        </AccessContext.Provider>
    );
}

export default AccessProvider;