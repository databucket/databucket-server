
const DataItemsReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_DATA_ITEMS":
            return {
                ...state,
                dataItems: action.payload
            };
        case "ADD_DATA_ITEM":
            return {
                ...state,
                dataItems: [...state.dataItems, action.payload]
            };
        case "EDIT_DATA_ITEM":
            const updatedData = action.payload;
            const updatedDataItems = state.data.map(data => {
                if (data.id === updatedData.id)
                    return updatedData;
                return data;
            });
            return {
                ...state,
                dataItems: updatedDataItems
            }
        case "REMOVE_DATA_ITEM":
            return {
                ...state,
                dataItems: state.dataItems.filter(data => data.id !== action.payload)
            };
        default:
            return state;
    }
};

export default DataItemsReducer;