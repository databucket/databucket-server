import {notifierChangeAdapter} from "../../utils/JsonHelper";

const TemplatesReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_TEMPLATES":
            return {
                ...state,
                templates: action.payload
            };
        case "ADD_TEMPLATE":
            return {
                ...state,
                templates: [...state.templates, action.payload]
            };
        case "EDIT_TEMPLATE":
            const updatedTemplate = action.payload;
            const updatedTemplates = state.templates.map(template => {
                if (template.id === updatedTemplate.id)
                    return updatedTemplate;
                return template;
            });
            return {
                ...state,
                templates: updatedTemplates
            }
        case "REMOVE_TEMPLATE":
            return {
                ...state,
                templates: state.templates.filter(template => template.id !== action.payload)
            };
        case "NOTIFY_TEMPLATES":
            return {
                ...state,
                templates: notifierChangeAdapter(state.templates, action.payload)
            };
        default:
            return state;
    }
};

export default TemplatesReducer;