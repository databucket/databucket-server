export default (state, action) => {
    switch (action.type) {
        case "FETCH_COLUMNS":
            return {
                ...state,
                columns: action.payload
            };
        case "ADD_COLUMNS":
            return {
                ...state,
                columns: [...state.columns, action.payload]
            };
        case "EDIT_COLUMNS":
            const updatedColumn = action.payload;
            const updatedColumns = state.columns.map(columns => {
                if (columns.id === updatedColumn.id)
                    return updatedColumn;
                return columns;
            });
            return {
                ...state,
                columns: updatedColumns
            };
        case "REMOVE_COLUMNS":
            return {
                ...state,
                columns: state.columns.filter(columns => columns.id !== action.payload)
            };
        default:
            return state;
    }
}