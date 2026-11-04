import {logOut} from "./ConfigurationStorage";

// API version 2 of the data endpoints: rules and logic translated safely (no SQL text); other endpoints ignore it
export const API_VERSION_HEADER = {'Databucket-Api-Version': '2'};

export function fetchHelper(token) {
    if (token) {
        return {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json',
            ...API_VERSION_HEADER
        };
    } else {
        return {'Content-Type': 'application/json', ...API_VERSION_HEADER};
    }
}

export function handleLoginErrors(response) {
    return response.text()
        .then(text => {
        const body = text !== '' ? JSON.parse(text) : JSON.parse("{}");
        if (!response.ok) {
            const error = body.message || body.error || response.statusText;
            return Promise.reject(error);
        }
        return body;
    });
}


export function handleErrors(response) {
    return response.text().then(text => {
        const body = text !== '' ? JSON.parse(text) : JSON.parse("{}");
        if (!response.ok) {
            if (response.status === 401) {
                logOut();
                window.location.reload();
            }

            const error = body.message || response.statusText;
            return Promise.reject(error);
        }
        return body;
    });
}
