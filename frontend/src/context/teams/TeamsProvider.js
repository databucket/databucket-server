import React, {useReducer} from 'react';
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import TeamsContext from "./TeamsContext";
import TeamsReducer from "./TeamsReducer";
import {handleErrors} from "../../utils/FetchHelper";
import {getTeamMapper} from "../../utils/NullValueMappers";
import {getBaseUrl} from "../../utils/UrlBuilder";

const TeamsProvider = props => {
    const initialState = {
        teams: null
    }

    const [state, dispatch] = useReducer(TeamsReducer, initialState);

    const fetchTeams = () => {
        fetch(getBaseUrl('teams'), getGetOptions())
            .then(handleErrors)
            .then(teams => dispatch({
                type: "FETCH_TEAMS",
                payload: convertNullValuesInCollection(teams, getTeamMapper())
            }))
            .catch(err => console.log(err));
    }

    const addTeam = (team) => {
        dispatch({
            type: "ADD_TEAM",
            payload: team
        });
    }

    const editTeam = (team) => {
        dispatch({
            type: "EDIT_TEAM",
            payload: team
        });
    }

    const removeTeam = (id) => {
        dispatch({
            type: "REMOVE_TEAM",
            payload: id
        });
    }

    const notifyTeams = (sourceName, sourceObjectId, sourceObjectItemsIds) => {
        console.log(`notifyTeams: ${sourceName}(${sourceObjectId}) -> ${sourceObjectItemsIds}`);
        let itemsTargetFieldName;
        switch (sourceName) {
            case "USER":
                itemsTargetFieldName = "usersIds";
                break;
            default:
                console.log("TeamsProvider - Undefined notification source! " + sourceName);
                return;
        }
        dispatch({
            type: "NOTIFY_TEAMS",
            payload: {itemsTargetFieldName: itemsTargetFieldName, sourceObjectId: sourceObjectId, sourceObjectItemsIds: sourceObjectItemsIds}
        });
    }

    return (
        <TeamsContext.Provider value={{teams: state.teams, fetchTeams, addTeam, editTeam, removeTeam, notifyTeams}}>
            {props.children}
        </TeamsContext.Provider>
    );
}

export default TeamsProvider;