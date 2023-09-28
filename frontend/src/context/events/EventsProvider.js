import React, {useMemo, useReducer} from 'react';
import EventsReducer from "./EventsReducer";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getEventsMapper} from "../../utils/NullValueMappers";
import EventsContext from "./EventsContext";
import {getBaseUrl} from "../../utils/UrlBuilder";

const EventsProvider = props => {

    const initialState = {
        events: null
    }

    const [state, dispatch] = useReducer(EventsReducer, initialState);

    const fetchEvents = () => {
        fetch(getBaseUrl('events'), getGetOptions())
            .then(handleErrors)
            .then(classes => dispatch({
                type: "FETCH_EVENTS",
                payload: convertNullValuesInCollection(classes, getEventsMapper())
            }))
            .catch(err => console.log(err));
    }

    const addEvent = (event) => {
        dispatch({
            type: "ADD_EVENT",
            payload: event
        });
    }

    const editEvent = (event) => {
        dispatch({
            type: "EDIT_EVENT",
            payload: event
        });
    }

    const removeEvent = (id) => {
        dispatch({
            type: "REMOVE_EVENT",
            payload: id
        });
    }

    const events = useMemo(() => {
        return {events: state.events, fetchEvents, addEvent, editEvent, removeEvent};
    }, [state.events]);
    return (
        <EventsContext.Provider value={events}>
            {props.children}
        </EventsContext.Provider>
    );
}

export default EventsProvider;
