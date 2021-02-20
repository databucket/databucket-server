import {notifierChangeAdapter} from "../../utils/JsonHelper";

export default (state, action) => {
    switch (action.type) {
        case "FETCH_USERS":
            return {
                ...state,
                users: action.payload
            };
        case "ADD_USER":
            return {
                ...state,
                users: [...state.users, action.payload]
            };
        case "EDIT_USER":
            const updatedUser = action.payload;
            const updatedUsers = state.users.map(user => {
                if (user.id === updatedUser.id)
                    return updatedUser;
                return user;
            });
            return {
                ...state,
                users: updatedUsers
            };
        case "NOTIFY_USERS":
            return {
                ...state,
                users: notifierChangeAdapter(state.users, action.payload)
            };
        default:
            return state;
    }
}