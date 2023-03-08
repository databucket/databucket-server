import React, {useMemo, useReducer} from 'react';
import FiltersReducer from "./FiltersReducer";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getFiltersMapper} from "../../utils/NullValueMappers";
import FiltersContext from "./FiltersContext";
import {getBaseUrl} from "../../utils/UrlBuilder";

const FiltersProvider = props => {

    const initialState = {
        filters: null
    }

    const [state, dispatch] = useReducer(FiltersReducer, initialState);

    const fetchFilters = () => {
        fetch(getBaseUrl('filters'), getGetOptions())
            .then(handleErrors)
            .then(classes => dispatch({
                type: "FETCH_FILTERS",
                payload: convertNullValuesInCollection(classes, getFiltersMapper())
            }))
            .catch(err => console.log(err));
    }

    const addFilter = (filter) => {
        dispatch({
            type: "ADD_FILTER",
            payload: filter
        });
    }

    const editFilter = (filter) => {
        dispatch({
            type: "EDIT_FILTER",
            payload: filter
        });
    }

    const removeFilter = (id) => {
        dispatch({
            type: "REMOVE_FILTER",
            payload: id
        });
    }

    const filters = useMemo(() => {
        return {filters: state.filters, fetchFilters, addFilter, editFilter, removeFilter};
    }, []);
    return (
        <FiltersContext.Provider value={filters}>
            {props.children}
        </FiltersContext.Provider>
    );
}

export default FiltersProvider;
