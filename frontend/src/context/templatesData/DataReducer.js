
const DataReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_DATA":
            return {
                ...state,
                data: action.payload
            };
        case "ADD_DATA":
            return {
                ...state,
                data: [...state.data, action.payload]
            };
        case "EDIT_DATA":
            const updatedData = action.payload;
            const updatedDataItems = state.data.map(data => {
                if (data.id === updatedData.id)
                    return updatedData;
                return data;
            });
            return {
                ...state,
                data: updatedDataItems
            }
        case "REMOVE_DATA":
            return {
                ...state,
                data: state.data.filter(data => data.id !== action.payload)
            };
        case "LAST_TEMPLATE":
            return {
                ...state,
                templateId: action.payload
            };
        default:
            return state;
    }
};

export default DataReducer;