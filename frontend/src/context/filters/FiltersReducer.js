const FiltersReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_FILTERS":
            return {
                ...state,
                filters: action.payload
            };
        case "ADD_FILTER":
            return {
                ...state,
                filters: [...state.filters, action.payload]
            };
        case "EDIT_FILTER":
            const updatedFilter = action.payload;
            const updatedFilters = state.filters.map(filters => {
                if (filters.id === updatedFilter.id)
                    return updatedFilter;
                return filters;
            });
            return {
                ...state,
                filters: updatedFilters
            };
        case "REMOVE_FILTER":
            return {
                ...state,
                filters: state.filters.filter(filter => filter.id !== action.payload)
            };
        default:
            return state;
    }
};

export default FiltersReducer;