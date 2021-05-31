const RolesReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_ROLES":
            return {
                ...state,
                roles: action.payload
            }
        default:
            return state;
    }
};

export default RolesReducer;