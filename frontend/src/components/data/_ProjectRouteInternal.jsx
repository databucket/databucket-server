import React from 'react';
import {Redirect, Switch} from "react-router-dom";
import ProjectRoute from "../../route/ProjectRoute";
import _ProjectSettingsTabs from "../settings/_ProjectSettingsTabs";
import {getProjectSettingsPath} from "../../route/AppRouter";
import ProjectDataWrapper from "./_ProjectDataWrapper";

export default function _ProjectRouteInternal() {

    return (
        <Switch>
            <Redirect exact from='/project/settings' to={getProjectSettingsPath()}/>
            <ProjectRoute path="/project/settings/:page" component={_ProjectSettingsTabs}/>
            <ProjectRoute path="/project" component={ProjectDataWrapper}/>
        </Switch>
    )
}
