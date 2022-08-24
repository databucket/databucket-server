import React, {useReducer} from 'react';
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import TemplatesContext from "./TemplatesContext";
import TemplatesReducer from "./TemplatesReducer";
import {handleErrors} from "../../utils/FetchHelper";
import {getBaseUrl} from "../../utils/UrlBuilder";
import {getTemplateMapper} from "../../utils/NullValueMappers";
import {getActiveProjectId} from "../../utils/ConfigurationStorage";

const TemplatesProvider = props => {
    const initialState = {
        templates: null
    }

    const [state, dispatch] = useReducer(TemplatesReducer, initialState);

    const fetchTemplates = () => {
        fetch(getBaseUrl('templates'), getGetOptions())
            .then(handleErrors)
            .then(templates => dispatch({
                type: "FETCH_TEMPLATES",
                payload: convertNullValuesInCollection(templates, getTemplateMapper())
            }))
            .catch(err => console.log(err));
    }

    const fetchProjectTemplates = () => {
        fetch(getBaseUrl('templates/project/' + getActiveProjectId()), getGetOptions())
            .then(handleErrors)
            .then(templates => dispatch({
                type: "FETCH_TEMPLATES",
                payload: convertNullValuesInCollection(templates, getTemplateMapper())
            }))
            .catch(err => console.log(err));
    }

    const addTemplate = (template) => {
        dispatch({
            type: "ADD_TEMPLATE",
            payload: template
        });
    }

    const editTemplate = (template) => {
        dispatch({
            type: "EDIT_TEMPLATE",
            payload: template
        });
    }

    const removeTemplate = (id) => {
        dispatch({
            type: "REMOVE_TEMPLATE",
            payload: id
        });
    }

    const notifyTemplates = (sourceName, sourceObjectId, sourceObjectItemsIds) => {
        let itemsTargetFieldName;
        switch (sourceName) {
            case "PROJECT":
                itemsTargetFieldName = "projectsIds";
                break;
            default:
                console.log("DataItemsProvider - Undefined notification source! " + sourceName);
                return;
        }
        dispatch({
            type: "NOTIFY_TEMPLATES",
            payload: {itemsTargetFieldName: itemsTargetFieldName, sourceObjectId: sourceObjectId, sourceObjectItemsIds: sourceObjectItemsIds}
        });
    }

    return (
        <TemplatesContext.Provider value={{templates: state.templates, fetchTemplates, fetchProjectTemplates, addTemplate, editTemplate, removeTemplate, notifyTemplates}}>
            {props.children}
        </TemplatesContext.Provider>
    );
}

export default TemplatesProvider;