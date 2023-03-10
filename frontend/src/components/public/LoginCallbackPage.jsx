import React, {useEffect} from "react";
import "./AuthPage.css"
import {getBaseUrl} from "../../utils/UrlBuilder";
import {handleSuccessfulLogin} from "../utils/AuthHelper";
import {useHistory} from "react-router-dom";
import {getActiveProjectId} from "../../utils/ConfigurationStorage";


export default function LoginCallbackPage() {

    const history = useHistory()

    const activeProjectId = getActiveProjectId();
    const targetUrl = getBaseUrl(`auth/user-info`)
    useEffect(() => {
        fetch(!!activeProjectId ? targetUrl + `?projectId=${activeProjectId}` : targetUrl, {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        })
            .then(value => value.json())
            .then(data => handleSuccessfulLogin(data, {projects: data.projects}))
            .then(value => {
                history.replace("/select-project", {projects: value.projects})
            })
            .catch(error => {
                console.error(error);
            })
    }, []);

    return (
        <></>
    );
}
