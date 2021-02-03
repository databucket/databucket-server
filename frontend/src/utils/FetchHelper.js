import {clearToken} from "./ConfigurationStorage";

export function fetchHelper(token) {
    if (token) {
        return {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
        };
    } else {
        return {'Content-Type': 'application/json'};
    }
}

export function handleErrors(response) {
    return response.text().then(text => {
        const body = text !== '' ? JSON.parse(text) : JSON.parse("{}");
        if (!response.ok) {
            if (response.status === 401) {
                clearToken();
            }

            const error = body.message || response.statusText;
            return Promise.reject(error);
        }
        return body;
    });
}