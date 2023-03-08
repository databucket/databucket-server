import React, {useMemo, useReducer} from 'react';
import AccessContext from "./AccessContext";
import AccessReducer from "./AccessReducer";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {getBaseUrl, getSessionUrl, getSessionUrlWithIds} from "../../utils/UrlBuilder";

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
        classes: null,
        tasks: null,
        tags: null,
        svgs: null,
        enums: null,
        users: null
    }

    const [state, dispatch] = useReducer(AccessReducer, initialState);

    const fetchAccessTree = () => {
        fetch(getSessionUrl('access-tree'), getGetOptions())
            .then(handleErrors)
            .then(accessTree => dispatch({type: "FETCH_ACCESS_TREE", payload: accessTree}))
            .catch(err => console.log(err));
    }

    const fetchSessionColumns = () => {
        const columnsIds = [...new Set(state.views.map(({columnsId}) => columnsId))];
        if (columnsIds.length > 0) {
            fetch(getSessionUrlWithIds('columns', columnsIds), getGetOptions())
                .then(handleErrors)
                .then(columns => dispatch({type: "FETCH_SESSION_COLUMNS", payload: columns}))
                .catch(err => console.log(err));
        } else {
            dispatch({type: "FETCH_SESSION_COLUMNS", payload: []});
        }
    }

    const fetchSessionFilters = () => {
        const viewFiltersIds = [...new Set(state.views.map(({filterId}) => filterId).filter(id => id != null))];
        const tasksFiltersIds = [...new Set(state.tasks.map(({filterId}) => filterId).filter(id => id != null))];
        const filtersIds = [...new Set([...viewFiltersIds, ...tasksFiltersIds])];
        if (filtersIds.length > 0) {
            fetch(getSessionUrlWithIds('filters', filtersIds), getGetOptions())
                .then(handleErrors)
                .then(filters => dispatch({type: "FETCH_SESSION_FILTERS", payload: filters}))
                .catch(err => console.log(err));
        } else {
            dispatch({type: "FETCH_SESSION_FILTERS", payload: []});
        }
    }

    const fetchSessionClasses = () => {
        fetch(getBaseUrl('classes'), getGetOptions())
            .then(handleErrors)
            .then(classes => dispatch({type: "FETCH_SESSION_CLASSES", payload: classes}))
            .catch(err => console.log(err));
    }

    const fetchSessionTasks = () => {
        fetch(getBaseUrl('tasks'), getGetOptions())
            .then(handleErrors)
            .then(tasks => dispatch({type: "FETCH_SESSION_TASKS", payload: tasks}))
            .catch(err => console.log(err));
    }

    const fetchSessionTags = () => {
        fetch(getBaseUrl('tags'), getGetOptions())
            .then(handleErrors)
            .then(tags => dispatch({type: "FETCH_SESSION_TAGS", payload: tags}))
            .catch(err => console.log(err));
    }

    const fetchSessionSvgs = () => {
        fetch(getBaseUrl('svg'), getGetOptions())
            .then(handleErrors)
            .then(svgs => dispatch({type: "FETCH_SESSION_SVGS", payload: svgs}))
            .catch(err => console.log(err));
    }

    const fetchSessionEnums = () => {
        fetch(getBaseUrl('enums'), getGetOptions())
            .then(handleErrors)
            .then(enums => dispatch({type: "FETCH_SESSION_ENUMS", payload: enums}))
            .catch(err => console.log(err));
    }

    const fetchSessionUsers = () => {
        fetch(getBaseUrl('users'), getGetOptions())
            .then(handleErrors)
            .then(users => dispatch({type: "FETCH_SESSION_USERS", payload: users}))
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

    const accessValue = useMemo(() => {
        return {
            activeGroup: state.activeGroup,
            activeBucket: state.activeBucket,
            bucketsTabs: state.bucketsTabs,
            projects: state.projects,
            groups: state.groups,
            buckets: state.buckets,
            views: state.views,
            columns: state.columns,
            filters: state.filters,
            classes: state.classes,
            tasks: state.tasks,
            tags: state.tags,
            svgs: state.svgs,
            enums: state.enums,
            users: state.users,
            fetchAccessTree,
            setActiveGroup,
            setActiveBucket,
            addTab,
            removeTab,
            fetchSessionColumns,
            fetchSessionFilters,
            fetchSessionClasses,
            fetchSessionTasks,
            fetchSessionTags,
            fetchSessionSvgs,
            fetchSessionEnums,
            fetchSessionUsers
        };
    }, [state]);
    return (
        <AccessContext.Provider value={accessValue}>
            {props.children}
        </AccessContext.Provider>
    );
}

export default AccessProvider;
