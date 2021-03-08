import React, {useReducer} from 'react';
import ViewsReducer from "./ViewsReducer";
import {getBaseUrl, getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getViewsMapper} from "../../utils/NullValueMappers";
import ViewsContext from "./ViewsContext";

const ViewsProvider = props => {

    const initialState = {
        views: null
    }

    const [state, dispatch] = useReducer(ViewsReducer, initialState);

    const fetchViews = () => {
        fetch(getBaseUrl('views'), getGetOptions())
            .then(handleErrors)
            .then(classes => dispatch({
                type: "FETCH_VIEWS",
                payload: convertNullValuesInCollection(classes, getViewsMapper())
            }))
            .catch(err => console.log(err));
    }

    const addView = (view) => {
        dispatch({
            type: "ADD_VIEW",
            payload: view
        });
    }

    const editView = (view) => {
        dispatch({
            type: "EDIT_VIEW",
            payload: view
        });
    }

    const removeView = (id) => {
        dispatch({
            type: "REMOVE_VIEW",
            payload: id
        });
    }

    const notifyViews = (sourceName, sourceObjectId, sourceObjectItemsIds) => {
        console.log(`notifyViews: ${sourceName}(${sourceObjectId}) -> ${sourceObjectItemsIds}`);
        let itemsTargetFieldName;
        switch (sourceName) {
            case "USER":
                itemsTargetFieldName = "usersIds";
                break;
            default:
                console.log("Undefined notification source!");
                return;
        }
        dispatch({
            type: "NOTIFY_VIEWS",
            payload: {itemsTargetFieldName: itemsTargetFieldName, sourceObjectId: sourceObjectId, sourceObjectItemsIds: sourceObjectItemsIds}
        });
    }

    return (
        <ViewsContext.Provider value={{views: state.views, fetchViews, addView, editView, removeView, notifyViews}}>
            {props.children}
        </ViewsContext.Provider>
    );
}

export default ViewsProvider;