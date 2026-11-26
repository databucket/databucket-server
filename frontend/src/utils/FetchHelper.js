import {logOut} from "./ConfigurationStorage";

// marks the requests of the web application, e.g. "WEB" as the source of a change in Settings > Activity
const CLIENT_HEADER = {'Databucket-Client': 'web'};

export function fetchHelper(token) {
    if (token) {
        return {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json',
            ...CLIENT_HEADER
        };
    } else {
        return {'Content-Type': 'application/json'};
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
