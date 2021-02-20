export default (state, action) => {
    switch (action.type) {
        case "FETCH_ENUMS":
            return {
                ...state,
                enums: action.payload
            };
        case "ADD_ENUM":
            return {
                ...state,
                enums: [...state.enums, action.payload]
            };
        case "EDIT_ENUM":
            const updatedEnum = action.payload;
            const updatedEnums = state.enums.map(item => {
                if (item.id === updatedEnum.id)
                    return updatedEnum;
                return item;
            });
            return {
                ...state,
                enums: updatedEnums
            };
        case "REMOVE_ENUM":
            return {
                ...state,
                enums: state.enums.filter(item => item.id !== action.payload)
            };
        default:
            return state;
    }
}