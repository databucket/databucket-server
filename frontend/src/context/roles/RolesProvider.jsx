import React, {useReducer} from 'react';
import {getGetOptions} from "../../utils/MaterialTableHelper";
import RolesContext from "./RolesContext";
import RolesReducer from "./RolesReducer";
import {handleErrors} from "../../utils/FetchHelper";
import {getBaseUrl} from "../../utils/UrlBuilder";

const RolesProvider = props => {
    const initialState = {
        roles: null
    }

    const [state, dispatch] = useReducer(RolesReducer, initialState);

    const fetchRoles = () => {
        fetch(getBaseUrl('roles'), getGetOptions())
            .then(handleErrors)
            .then(roles => dispatch({
                type: "FETCH_ROLES",
                payload: roles
            }))
            .catch(err => console.log(err));
    }

    return (
        <RolesContext.Provider value={{roles: state.roles, fetchRoles}}>
            {props.children}
        </RolesContext.Provider>
    );
}

export default RolesProvider;