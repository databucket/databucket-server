import React, {useReducer} from 'react';
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import DataContext from "./DataContext";
import DataReducer from "./DataReducer";
import {handleErrors} from "../../utils/FetchHelper";
import {getBaseUrl} from "../../utils/UrlBuilder";
import {getTemplateMapper} from "../../utils/NullValueMappers";

const DataProvider = props => {
    const initialState = {
        data: null,
        templateId: null
    }

    const [state, dispatch] = useReducer(DataReducer, initialState);

    const fetchData = (templateId) => {
        fetch(getBaseUrl('templates/' + templateId + '/data'), getGetOptions())
            .then(handleErrors)
            .then(data => dispatch({
                type: "FETCH_DATA",
                payload: convertNullValuesInCollection(data, getTemplateMapper())
            }))
            .then(() => dispatch({
                type: "LAST_TEMPLATE",
                payload: templateId
            }))
            .catch(err => console.log(err));
    }

    const addData = (data) => {
        dispatch({
            type: "ADD_DATA",
            payload: data
        });
    }

    const editData = (data) => {
        dispatch({
            type: "EDIT_DATA",
            payload: data
        });
    }

    const removeData = (id) => {
        dispatch({
            type: "REMOVE_DATA",
            payload: id
        });
    }

    return (
        <DataContext.Provider value={{data: state.data, templateId: state.templateId, fetchData, addData, editData, removeData}}>
            {props.children}
        </DataContext.Provider>
    );
}

export default DataProvider;