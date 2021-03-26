import React, {useReducer} from 'react';
import EnumsReducer from "./EnumsReducer";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getEnumMapper} from "../../utils/NullValueMappers";
import EnumsContext from "./EnumsContext";
import {getBaseUrl} from "../../utils/UrlBuilder";

const EnumsProvider = props => {

    const initialState = {
        enums: null
    }

    const [state, dispatch] = useReducer(EnumsReducer, initialState);

    const fetchEnums = () => {
        fetch(getBaseUrl('enums'), getGetOptions())
            .then(handleErrors)
            .then(classes => dispatch({
                type: "FETCH_ENUMS",
                payload: convertNullValuesInCollection(classes, getEnumMapper())
            }))
            .catch(err => console.log(err));
    }

    const addEnum = (dataEnum) => {
        dispatch({
            type: "ADD_ENUM",
            payload: dataEnum
        });
    }

    const editEnum = (dataEnum) => {
        dispatch({
            type: "EDIT_ENUM",
            payload: dataEnum
        });
    }

    const removeEnum = (id) => {
        dispatch({
            type: "REMOVE_ENUM",
            payload: id
        });
    }

    return (
        <EnumsContext.Provider value={{enums: state.enums, fetchEnums, addEnum, editEnum, removeEnum}}>
            {props.children}
        </EnumsContext.Provider>
    );
}

export default EnumsProvider;