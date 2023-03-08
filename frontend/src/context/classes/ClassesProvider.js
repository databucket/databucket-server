import React, {useMemo, useReducer} from 'react';
import ClassesReducer from "./ClassesReducer";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getClassMapper} from "../../utils/NullValueMappers";
import ClassesContext from "./ClassesContext";
import {getBaseUrl} from "../../utils/UrlBuilder";

const ClassesProvider = props => {

    const initialState = {
        classes: null,
        classesLookup: null
    }

    const [state, dispatch] = useReducer(ClassesReducer, initialState);

    const fetchClasses = () => {
        fetch(getBaseUrl('classes'), getGetOptions())
            .then(handleErrors)
            .then(classes => dispatch({
                type: "FETCH_CLASSES",
                payload: convertNullValuesInCollection(classes, getClassMapper())
            }))
            .catch(err => console.log(err));
    }

    const addClass = (dataClass) => {
        dispatch({
            type: "ADD_CLASS",
            payload: dataClass
        });
    }

    const editClass = (dataClass) => {
        dispatch({
            type: "EDIT_CLASS",
            payload: dataClass
        });
    }

    const removeClass = (id) => {
        dispatch({
            type: "REMOVE_CLASS",
            payload: id
        });
    }

    const classes = useMemo(() => {
        return {
            classes: state.classes,
            fetchClasses,
            addClass,
            editClass,
            removeClass,
            classesLookup: state.classesLookup
        }
    }, [state.classes, state.classesLookup]);
    return (
        <ClassesContext.Provider value={classes}>
            {props.children}
        </ClassesContext.Provider>
    );
}

export default ClassesProvider;
