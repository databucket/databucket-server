import React, {useMemo, useReducer} from 'react';
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import GroupsContext from "./GroupsContext";
import GroupsReducer from "./GroupsReducer";
import {handleErrors} from "../../utils/FetchHelper";
import {getGroupMapper} from "../../utils/NullValueMappers";
import {getBaseUrl} from "../../utils/UrlBuilder";

const GroupsProvider = props => {
    const initialState = {
        groups: null
    }

    const [state, dispatch] = useReducer(GroupsReducer, initialState);

    const fetchGroups = () => {
        fetch(getBaseUrl('groups'), getGetOptions())
            .then(handleErrors)
            .then(groups => dispatch({
                type: "FETCH_GROUPS",
                payload: convertNullValuesInCollection(groups, getGroupMapper())
            }))
            .catch(err => console.log(err));
    }

    const addGroup = (group) => {
        dispatch({
            type: "ADD_GROUP",
            payload: group
        });
    }

    const editGroup = (group) => {
        dispatch({
            type: "EDIT_GROUP",
            payload: group
        });
    }

    const removeGroup = (id) => {
        dispatch({
            type: "REMOVE_GROUP",
            payload: id
        });
    }

    const notifyGroups = (sourceName, sourceObjectId, sourceObjectItemsIds) => {
        // console.log(`notifyGroups: ${sourceName}(${sourceObjectId}) -> ${sourceObjectItemsIds}`);
        let itemsTargetFieldName;
        switch (sourceName) {
            case "USER":
                itemsTargetFieldName = "usersIds";
                break;
            case "BUCKET":
                itemsTargetFieldName = "bucketsIds";
                break;
            case "TEAM":
                itemsTargetFieldName = "teamsIds";
                break;
            default:
                console.log("GroupsProvider - Undefined notification source! " + sourceName);
                return;
        }
        dispatch({
            type: "NOTIFY_GROUPS",
            payload: {
                itemsTargetFieldName: itemsTargetFieldName,
                sourceObjectId: sourceObjectId,
                sourceObjectItemsIds: sourceObjectItemsIds
            }
        });
    }

    const groups = useMemo(() => {
        return {groups: state.groups, fetchGroups, addGroup, editGroup, removeGroup, notifyGroups};
    }, [state.groups]);
    return (
        <GroupsContext.Provider value={groups}>
            {props.children}
        </GroupsContext.Provider>
    );
}

export default GroupsProvider;
