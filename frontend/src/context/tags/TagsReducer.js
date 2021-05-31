import {notifierChangeAdapter} from "../../utils/JsonHelper";

const TagReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_TAGS":
            return {
                ...state,
                tags: action.payload
            };
        case "ADD_TAG":
            return {
                ...state,
                tags: [...state.tags, action.payload]
            };
        case "EDIT_TAG":
            const updatedTag = action.payload;
            const updatedTags = state.tags.map(tag => {
                if (tag.id === updatedTag.id)
                    return updatedTag;
                return tag;
            });
            return {
                ...state,
                tags: updatedTags
            };
        case "REMOVE_TAG":
            return {
                ...state,
                tags: state.tags.filter(tag => tag.id !== action.payload)
            };
        case "NOTIFY_TAGS":
            return {
                ...state,
                tags: notifierChangeAdapter(state.tags, action.payload)
            };
        default:
            return state;
    }
};

export default TagReducer;