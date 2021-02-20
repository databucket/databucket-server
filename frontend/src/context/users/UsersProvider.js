import React, {useReducer} from 'react';
import {getBaseUrl, getGetOptions} from "../../utils/MaterialTableHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import UsersContext from "./UsersContext";
import UserReducer from "./UsersReducer";
import {handleErrors} from "../../utils/FetchHelper";
import {getUserMapper} from "../../utils/NullValueMappers";


const UsersProvider = props => {
    const initialState = {
        users: null
    }

    const [state, dispatch] = useReducer(UserReducer, initialState);

    const fetchUsers = () => {
        fetch(getBaseUrl('users'), getGetOptions())
            .then(handleErrors)
            .then(users => dispatch({
                type: "FETCH_USERS",
                payload: convertNullValuesInCollection(users, getUserMapper())
            }))
            .catch(err => console.log(err));
    }

    const editUser = (user) => {
        dispatch({
            type: "EDIT_USER",
            payload: user
        });
    }

    const notifyUsers = (sourceName, sourceObjectId, sourceObjectItemsIds) => {
        console.log(`notifyUsers: ${sourceName}(${sourceObjectId}) -> ${sourceObjectItemsIds}`);
        let itemsTargetFieldName;
        switch (sourceName) {
            case "GROUP":
                itemsTargetFieldName = "groupsIds";
                break;
            case "BUCKET":
                itemsTargetFieldName = "bucketsIds";
                break;
            default:
                console.log("Undefined notification source!");
                return;
        }
        dispatch({
            type: "NOTIFY_USERS",
            payload: {itemsTargetFieldName: itemsTargetFieldName, sourceObjectId: sourceObjectId, sourceObjectItemsIds: sourceObjectItemsIds}
        });
    }

    return (
        <UsersContext.Provider value={{users: state.users, fetchUsers, editUser, notifyUsers}}>
            {props.children}
        </UsersContext.Provider>
    );
}

export default UsersProvider;