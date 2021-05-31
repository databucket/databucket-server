import {notifierChangeAdapter} from "../../utils/JsonHelper";

const ViewsReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_VIEWS":
            return {
                ...state,
                views: action.payload
            };
        case "ADD_VIEW":
            return {
                ...state,
                views: [...state.views, action.payload]
            };
        case "EDIT_VIEW":
            const updatedView = action.payload;
            const updatedViews = state.views.map(view => {
                if (view.id === updatedView.id)
                    return updatedView;
                return view;
            });
            return {
                ...state,
                views: updatedViews
            };
        case "REMOVE_VIEW":
            return {
                ...state,
                views: state.views.filter(view => view.id !== action.payload)
            };
        case "NOTIFY_VIEWS":
            return {
                ...state,
                views: notifierChangeAdapter(state.views, action.payload)
            };
        default:
            return state;
    }
};

export default ViewsReducer;