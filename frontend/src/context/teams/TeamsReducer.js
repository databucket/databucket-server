import {notifierChangeAdapter} from "../../utils/JsonHelper";

const TeamsReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_TEAMS":
            return {
                ...state,
                teams: action.payload
            };
        case "ADD_TEAM":
            return {
                ...state,
                teams: [...state.teams, action.payload]
            };
        case "EDIT_TEAM":
            const updatedTeam = action.payload;
            const updatedTeams = state.teams.map(team => {
                if (team.id === updatedTeam.id)
                    return updatedTeam;
                return team;
            });
            return {
                ...state,
                teams: updatedTeams
            };
        case "REMOVE_TEAM":
            return {
                ...state,
                teams: state.teams.filter(team => team.id !== action.payload)
            };
        case "NOTIFY_TEAMS":
            return {
                ...state,
                teams: notifierChangeAdapter(state.teams, action.payload)
            };
        default:
            return state;
    }
};

export default TeamsReducer;