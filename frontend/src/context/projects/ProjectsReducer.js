import {notifierChangeAdapter} from "../../utils/JsonHelper";

const ProjectReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_PROJECTS":
            return {
                ...state,
                projects: action.payload
            };
        case "ADD_PROJECT":
            return {
                ...state,
                projects: [...state.projects, action.payload]
            };
        case "EDIT_PROJECT":
            const updatedProject = action.payload;
            const updatedProjects = state.projects.map(project => {
                if (project.id === updatedProject.id)
                    return updatedProject;
                return project;
            });
            return {
                ...state,
                projects: updatedProjects
            }
        case "REMOVE_PROJECT":
            return {
                ...state,
                projects: state.projects.filter(project => project.id !== action.payload)
            };
        case "NOTIFY_PROJECTS":
            return {
                ...state,
                projects: notifierChangeAdapter(state.projects, action.payload)
            };
        default:
            return state;
    }
};

export default ProjectReducer;