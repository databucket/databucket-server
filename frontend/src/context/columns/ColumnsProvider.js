import React, {useReducer} from 'react';
import ColumnsReducer from "./ColumnsReducer";
import {getBaseUrl, getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getColumnsMapper} from "../../utils/NullValueMappers";
import ColumnsContext from "./ColumnsContext";

const ColumnsProvider = props => {

    const initialState = {
        columns: null
    }

    const [state, dispatch] = useReducer(ColumnsReducer, initialState);

    const fetchColumns = () => {
        fetch(getBaseUrl('columns'), getGetOptions())
            .then(handleErrors)
            .then(classes => dispatch({
                type: "FETCH_COLUMNS",
                payload: convertNullValuesInCollection(classes, getColumnsMapper())
            }))
            .catch(err => console.log(err));
    }

    const addColumns = (columns) => {
        dispatch({
            type: "ADD_COLUMNS",
            payload: columns
        });
    }

    const editColumns = (columns) => {
        dispatch({
            type: "EDIT_COLUMNS",
            payload: columns
        });
    }

    const removeColumns = (id) => {
        dispatch({
            type: "REMOVE_COLUMNS",
            payload: id
        });
    }

    return (
        <ColumnsContext.Provider value={{columns: state.columns, fetchColumns, addColumns, editColumns, removeColumns}}>
            {props.children}
        </ColumnsContext.Provider>
    );
}

export default ColumnsProvider;