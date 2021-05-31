const EventsReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_EVENTS":
            return {
                ...state,
                events: action.payload
            };
        case "ADD_EVENT":
            return {
                ...state,
                events: [...state.events, action.payload]
            };
        case "EDIT_EVENT":
            const updatedEvent = action.payload;
            const updatedEvents = state.events.map(event => {
                if (event.id === updatedEvent.id)
                    return updatedEvent;
                return event;
            });
            return {
                ...state,
                events: updatedEvents
            };
        case "REMOVE_EVENT":
            return {
                ...state,
                events: state.events.filter(event => event.id !== action.payload)
            };
        default:
            return state;
    }
};

export default EventsReducer;