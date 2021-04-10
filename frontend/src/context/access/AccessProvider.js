import React, {useReducer} from 'react';
import AccessContext from "./AccessContext";
import AccessReducer from "./AccessReducer";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {getBaseUrl, getBaseUrlWithIds} from "../../utils/UrlBuilder";


const AccessProvider = props => {

    const initialState = {
        activeGroup: null,
        activeBucket: null,
        bucketsTabs: [],
        projects: null,
        groups: null,
        buckets: null,
        views: null,
        columns: null,
        filters: null,
        tasks: null,
        tags: null
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

    const fetchSessionColumns = () => {
        const columnsIds = [...new Set(state.views.map(({columnsId}) => columnsId))];
        if (columnsIds.length > 0)
            fetch(getBaseUrlWithIds('users/columns', columnsIds), getGetOptions())
                .then(handleErrors)
                .then(columns => dispatch({
                    type: "FETCH_SESSION_COLUMNS",
                    payload: columns
                }))
                .catch(err => console.log(err));
    }

    const fetchSessionFilters = () => {
        const filtersIds = [...new Set(state.views.map(({filterId}) => filterId).filter(id => id != null))];
        if (filtersIds.length > 0)
            fetch(getBaseUrlWithIds('users/filters', filtersIds), getGetOptions())
                .then(handleErrors)
                .then(filters => dispatch({
                    type: "FETCH_SESSION_FILTERS",
                    payload: filters
                }))
                .catch(err => console.log(err));
    }

    const fetchSessionTasks = () => {
        // TODO find all tasks ids that must be loaded
        fetch(getBaseUrl('users/tasks'), getGetOptions())
            .then(handleErrors)
            .then(tasks => dispatch({
                type: "FETCH_SESSION_TASKS",
                payload: tasks
            }))
            .catch(err => console.log(err));
    }

    const fetchSessionTags = () => {
        // TODO find all tags ids that must be loaded
        fetch(getBaseUrl('users/tags'), getGetOptions())
            .then(handleErrors)
            .then(tags => dispatch({
                type: "FETCH_SESSION_TAGS",
                payload: tags
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
                activeGroup: state.activeGroup,
                activeBucket: state.activeBucket,
                bucketsTabs: state.bucketsTabs,
                projects: state.projects,
                groups: state.groups,
                buckets: state.buckets,
                views: state.views,
                columns: state.columns,
                filters: state.filters,
                tasks: state.tasks,
                fetchAccessTree,
                setActiveGroup,
                setActiveBucket,
                addTab,
                removeTab,
                fetchSessionColumns,
                fetchSessionFilters,
                fetchSessionTasks,
                fetchSessionTags
            }
        }>
            {props.children}
        </AccessContext.Provider>
    );
}

export default AccessProvider;