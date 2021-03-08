import React, {useReducer} from 'react';
import TasksReducer from "./TasksReducer";
import {getBaseUrl, getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getTasksMapper} from "../../utils/NullValueMappers";
import TasksContext from "./TasksContext";

const TasksProvider = props => {

    const initialState = {
        tasks: null
    }

    const [state, dispatch] = useReducer(TasksReducer, initialState);

    const fetchTasks = () => {
        fetch(getBaseUrl('tasks'), getGetOptions())
            .then(handleErrors)
            .then(classes => dispatch({
                type: "FETCH_TASKS",
                payload: convertNullValuesInCollection(classes, getTasksMapper())
            }))
            .catch(err => console.log(err));
    }

    const addTask = (filter) => {
        dispatch({
            type: "ADD_TASK",
            payload: filter
        });
    }

    const editTask = (filter) => {
        dispatch({
            type: "EDIT_TASK",
            payload: filter
        });
    }

    const removeTask = (id) => {
        dispatch({
            type: "REMOVE_TASK",
            payload: id
        });
    }

    return (
        <TasksContext.Provider value={{tasks: state.tasks, fetchTasks, addTask, editTask, removeTask}}>
            {props.children}
        </TasksContext.Provider>
    );
}

export default TasksProvider;