import React, {useReducer} from 'react';
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import DataItemsContext from "./DataItemsContext";
import DataItemsReducer from "./DataItemsReducer";
import {handleErrors} from "../../utils/FetchHelper";
import {getBaseUrl} from "../../utils/UrlBuilder";
import {getTemplateMapper} from "../../utils/NullValueMappers";

const DataItemsProvider = props => {
    const initialState = {
        dataItems: null
    }

    const [state, dispatch] = useReducer(DataItemsReducer, initialState);

    const fetchDataItems = (dataId) => {
        fetch(getBaseUrl('templates/data/' + dataId + '/items'), getGetOptions())
            .then(handleErrors)
            .then(data => dispatch({
                type: "FETCH_DATA_ITEMS",
                payload: convertNullValuesInCollection(data, getTemplateMapper())
            }))
            .catch(err => console.log(err));
    }

    const addDataItem = (dataItem) => {
        dispatch({
            type: "ADD_DATA_ITEM",
            payload: dataItem
        });
    }

    const editDataItem = (dataItem) => {
        dispatch({
            type: "EDIT_DATA_ITEM",
            payload: dataItem
        });
    }

    const removeData = (id) => {
        dispatch({
            type: "REMOVE_DATA_ITEM",
            payload: id
        });
    }

    return (
        <DataItemsContext.Provider value={{dataItems: state.dataItems, fetchDataItems, addDataItem, editDataItem, removeData}}>
            {props.children}
        </DataItemsContext.Provider>
    );
}

export default DataItemsProvider;