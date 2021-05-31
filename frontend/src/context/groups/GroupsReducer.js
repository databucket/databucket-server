import {notifierChangeAdapter} from "../../utils/JsonHelper";

const GroupsReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_GROUPS":
            return {
                ...state,
                groups: action.payload
            };
        case "ADD_GROUP":
            return {
                ...state,
                groups: [...state.groups, action.payload]
            };
        case "EDIT_GROUP":
            const updatedGroup = action.payload;
            const updatedGroups = state.groups.map(group => {
                if (group.id === updatedGroup.id)
                    return updatedGroup;
                return group;
            });
            return {
                ...state,
                groups: updatedGroups
            };
        case "REMOVE_GROUP":
            return {
                ...state,
                groups: state.groups.filter(group => group.id !== action.payload)
            };
        case "NOTIFY_GROUPS":
            return {
                ...state,
                groups: notifierChangeAdapter(state.groups, action.payload)
            };
        default:
            return state;
    }
};

export default GroupsReducer;