import React, {useMemo, useReducer} from 'react';
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
            .then(enums => dispatch({
                type: "FETCH_ENUMS",
                payload: convertNullValuesInCollection(enums, getEnumMapper())
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

    const enumContext = useMemo(() => {
        return {enums: state.enums, fetchEnums, addEnum, editEnum, removeEnum};
    }, [state]);
    return (
        <EnumsContext.Provider value={enumContext}>
            {props.children}
        </EnumsContext.Provider>
    );
}

export default EnumsProvider;
