import React, {useMemo, useReducer} from 'react';
import ViewsReducer from "./ViewsReducer";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getViewsMapper} from "../../utils/NullValueMappers";
import ViewsContext from "./ViewsContext";
import {getBaseUrl} from "../../utils/UrlBuilder";

const ViewsProvider = props => {

    const initialState = {
        views: null
    }

    const [state, dispatch] = useReducer(ViewsReducer, initialState);

    const fetchViews = () => {
        fetch(getBaseUrl('views'), getGetOptions())
            .then(handleErrors)
            .then(views => dispatch({
                type: "FETCH_VIEWS",
                payload: convertNullValuesInCollection(views, getViewsMapper())
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
        // console.log(`notifyViews: ${sourceName}(${sourceObjectId}) -> ${sourceObjectItemsIds}`);
        let itemsTargetFieldName;
        switch (sourceName) {
            case "USER":
                itemsTargetFieldName = "usersIds";
                break;
            case "TEAM":
                itemsTargetFieldName = "teamsIds";
                break;
            default:
                console.log("ViewsProvider - Undefined notification source! " + sourceName);
                return;
        }
        dispatch({
            type: "NOTIFY_VIEWS",
            payload: {
                itemsTargetFieldName: itemsTargetFieldName,
                sourceObjectId: sourceObjectId,
                sourceObjectItemsIds: sourceObjectItemsIds
            }
        });
    }

    const views = useMemo(() => {
        return {views: state.views, fetchViews, addView, editView, removeView, notifyViews};
    }, [state.views]);
    return (
        <ViewsContext.Provider value={views}>
            {props.children}
        </ViewsContext.Provider>
    );
}

export default ViewsProvider;
