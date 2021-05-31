import {getClassesLookup} from "../../utils/LookupHelper";

const ClassesReducer = (state, action) => {
    let updatedClasses;
    switch (action.type) {
        case "FETCH_CLASSES":
            updatedClasses = action.payload;
            return {
                ...state,
                classes: updatedClasses,
                classesLookup: getClassesLookup(updatedClasses)
            };
        case "ADD_CLASS":
            updatedClasses = [...state.classes, action.payload];
            return {
                ...state,
                classes: updatedClasses,
                classesLookup: getClassesLookup(updatedClasses)
            };
        case "EDIT_CLASS":
            const updatedClass = action.payload;
            updatedClasses = state.classes.map(dataClass => {
                if (dataClass.id === updatedClass.id)
                    return updatedClass;
                return dataClass;
            });
            return {
                ...state,
                classes: updatedClasses,
                classesLookup: getClassesLookup(updatedClasses)
            }
        case "REMOVE_CLASS":
            updatedClasses = state.classes.filter(dataClass => dataClass.id !== action.payload);
            return {
                ...state,
                classes: updatedClasses,
                classesLookup: getClassesLookup(updatedClasses)
            }
        default:
            return state;
    }
};

export default ClassesReducer;