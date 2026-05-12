import React, {useReducer} from 'react';
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import ManageUsersContext from "./ManageUsersContext";
import UserReducer from "./UsersReducer";
import {handleErrors} from "../../utils/FetchHelper";
import {getManageUserMapper} from "../../utils/NullValueMappers";
import {getBaseUrl} from "../../utils/UrlBuilder";


const ManageUsersProvider = props => {
    const initialState = {
        users: null
    }

    const [state, dispatch] = useReducer(UserReducer, initialState);

    const fetchUsers = () => {
        fetch(getBaseUrl('manage/users'), getGetOptions())
            .then(handleErrors)
            .then(users => dispatch({
                type: "FETCH_USERS",
                payload: convertNullValuesInCollection(users, getManageUserMapper())
            }))
            .catch(err => console.log(err));
    }

    const addUser = (user) => {
        dispatch({
            type: "ADD_USER",
            payload: user
        });
    }

    const editUser = (user) => {
        dispatch({
            type: "EDIT_USER",
            payload: user
        });
    }

    const notifyUsers = (sourceName, sourceObjectId, sourceObjectItemsIds) => {
        let itemsTargetFieldName;
        switch (sourceName) {
            case "PROJECT":
                itemsTargetFieldName = "projectsIds";
                break;
            default:
                console.log("ManageUsersProvider - Undefined notification source! " + sourceName);
                return;
        }
        dispatch({
            type: "NOTIFY_USERS",
            payload: {itemsTargetFieldName: itemsTargetFieldName, sourceObjectId: sourceObjectId, sourceObjectItemsIds: sourceObjectItemsIds}
        });
    }

    return (
        <ManageUsersContext.Provider value={{users: state.users, fetchUsers, addUser, editUser, notifyUsers}}>
            {props.children}
        </ManageUsersContext.Provider>
    );
}

export default ManageUsersProvider;