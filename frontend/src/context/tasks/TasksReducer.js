const TaskReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_TASKS":
            return {
                ...state,
                tasks: action.payload
            };
        case "ADD_TASK":
            return {
                ...state,
                tasks: [...state.tasks, action.payload]
            };
        case "EDIT_TASK":
            const updatedTask = action.payload;
            const updatedTasks = state.tasks.map(tasks => {
                if (tasks.id === updatedTask.id)
                    return updatedTask;
                return tasks;
            });
            return {
                ...state,
                tasks: updatedTasks
            };
        case "REMOVE_TASK":
            return {
                ...state,
                tasks: state.tasks.filter(task => task.id !== action.payload)
            };
        default:
            return state;
    }
};

export default TaskReducer;