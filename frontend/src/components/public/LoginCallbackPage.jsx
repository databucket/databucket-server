import React, {useEffect, useMemo} from "react";
import {getBaseUrl} from "../../utils/UrlBuilder";
import {handleSuccessfulLogin} from "../utils/AuthHelper";
import {useHistory, useLocation} from "react-router-dom";
import {getActiveProjectId} from "../../utils/ConfigurationStorage";
import {fetchHelper} from "../../utils/FetchHelper";


export default function LoginCallbackPage() {

    const history = useHistory();
    const {search} = useLocation();
    const query = useMemo(() => new URLSearchParams(search), [search]);

    const activeProjectId = getActiveProjectId();
    const targetUrl = getBaseUrl(`auth/user-info`)
    useEffect(() => {
        fetch(!!activeProjectId ? targetUrl + `?projectId=${activeProjectId}` : targetUrl, {
            method: 'GET',
            headers: fetchHelper(query.get("token"))
        })
            .then(value => value.json())
            .then(data => handleSuccessfulLogin(data, {projects: data.projects}))
            .then(value => {
                if (value.projectId == null ) {
                    history.replace("/select-project", {projects: value.projects})
                } else {
                    history.push(`/project/${value.projectId}`)
                }
            })
            .catch(error => {
                console.error(error);
            })
    }, []);

    return (
        <></>
    );
}
