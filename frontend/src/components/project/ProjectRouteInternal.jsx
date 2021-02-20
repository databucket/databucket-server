import React, {useEffect} from 'react';
import {Redirect, Switch, useParams} from "react-router-dom";
import {getProjectId} from "../../utils/ConfigurationStorage";
import ProjectRoute from "../../route/ProjectRoute";
import _ProjectSettingsTabs from "./settings/_ProjectSettingsTabs";
import ProjectDataPage from "./data/ProjectDataPage";
import {getProjectSettingsPath} from "../../route/AppRouter";

export default function ProjectRouteInternal() {

    let { id } = useParams();

    useEffect(() => {
        if (id !== getProjectId()) {
            // komunikat, że jestem zalogowany do innego projektu
            // jeśli user chce się przelogować to wtedy:
            // zapamiętujemy link
            // robimy logowanie
            // otwieramy link
            // jeśli user nie chce się przelogowywać to przechodzimy do domyślnej ścieżki projektu
        }
    }, [id]);

    return (
        <Switch>
            <Redirect exact from='/project/:id/settings' to={getProjectSettingsPath()}/>
            <ProjectRoute path="/project/:id/settings/:page" component={_ProjectSettingsTabs}/>
            <ProjectRoute path="/project/:id" component={ProjectDataPage}/>
        </Switch>
    )
}
