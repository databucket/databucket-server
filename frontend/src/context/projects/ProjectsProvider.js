import React, {useMemo, useReducer} from 'react';
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import ProjectsContext from "./ProjectsContext";
import ProjectsReducer from "./ProjectsReducer";
import {handleErrors} from "../../utils/FetchHelper";
import {getManageProjectMapper} from "../../utils/NullValueMappers";
import {getBaseUrl} from "../../utils/UrlBuilder";

const ProjectsProvider = props => {
    const initialState = {
        projects: null
    }

    const [state, dispatch] = useReducer(ProjectsReducer, initialState);

    const fetchProjects = () => {
        fetch(getBaseUrl('manage/projects'), getGetOptions())
            .then(handleErrors)
            .then(projects => dispatch({
                type: "FETCH_PROJECTS",
                payload: convertNullValuesInCollection(projects, getManageProjectMapper())
            }))
            .catch(err => console.log(err));
    }

    const addProject = (project) => {
        dispatch({
            type: "ADD_PROJECT",
            payload: project
        });
    }

    const editProject = (project) => {
        dispatch({
            type: "EDIT_PROJECT",
            payload: project
        });
    }

    const removeProject = (id) => {
        dispatch({
            type: "REMOVE_PROJECT",
            payload: id
        });
    }

    const notifyProjects = (sourceName, sourceObjectId, sourceObjectItemsIds) => {
        let itemsTargetFieldName;
        switch (sourceName) {
            case "USER":
                itemsTargetFieldName = "usersIds";
                break;
            case "TEMPLATE":
                itemsTargetFieldName = "templatesIds";
                break;
            default:
                console.log("ProjectsProvider - Undefined notification source! " + sourceName);
                return;
        }
        dispatch({
            type: "NOTIFY_PROJECTS",
            payload: {
                itemsTargetFieldName: itemsTargetFieldName,
                sourceObjectId: sourceObjectId,
                sourceObjectItemsIds: sourceObjectItemsIds
            }
        });
    }

    const projects = useMemo(() => {
        return {projects: state.projects, fetchProjects, addProject, editProject, removeProject, notifyProjects};
    }, [state.projects]);
    return (
        <ProjectsContext.Provider value={projects}>
            {props.children}
        </ProjectsContext.Provider>
    );
}

export default ProjectsProvider;
