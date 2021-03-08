import React from 'react';
import {Redirect, Switch} from "react-router-dom";
import ProjectRoute from "../../route/ProjectRoute";
import _ProjectSettingsTabs from "./settings/_ProjectSettingsTabs";
import _ProjectDataPage from "./data/_ProjectDataPage";
import {getProjectSettingsPath} from "../../route/AppRouter";

export default function ProjectRouteInternal() {

    // useEffect(() => {
    //     if (id !== getActiveProjectId()) {
    //         // komunikat, że jestem zalogowany do innego projektu
    //         // jeśli user chce się przelogować to wtedy:
    //         // zapamiętujemy link
    //         // robimy logowanie
    //         // otwieramy link
    //         // jeśli user nie chce się przelogowywać to przechodzimy do domyślnej ścieżki projektu
    //     }
    // }, [id]);

    return (
        <Switch>
            <Redirect exact from='/project/settings' to={getProjectSettingsPath()}/>
            <ProjectRoute path="/project/settings/:page" component={_ProjectSettingsTabs}/>
            <ProjectRoute path="/project" component={_ProjectDataPage}/>
        </Switch>
    )
}
