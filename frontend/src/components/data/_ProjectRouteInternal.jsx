import React from 'react';
import {Redirect, Switch} from "react-router-dom";
import ProjectRoute from "../../route/ProjectRoute";
import _ProjectSettingsTabs from "../settings/_ProjectSettingsTabs";
import {getProjectSettingsPath} from "../../route/AppRouter";
import ProjectDataWrapper from "./_ProjectDataWrapper";
import DataDetailsPageWrapper from "./details_page/DataDetailsPageWrapper";
import NotFoundPage from "../NotFoundPage";
import PublicRoute from "../../route/PublicRoute";

export default function _ProjectRouteInternal() {

    return (
        <Switch>
            <Redirect exact from='/project/:projectId/settings' to={getProjectSettingsPath()}/>
            <ProjectRoute path="/project/:projectId/settings/:page" component={_ProjectSettingsTabs}/>
            <ProjectRoute path="/project/:projectId/bucket/:bucketName/data/:dataId/:jsonPath" component={DataDetailsPageWrapper}/>
            <ProjectRoute path="/project/:projectId/bucket/:bucketName/data/:dataId" component={DataDetailsPageWrapper}/>
            <ProjectRoute path="/project" component={ProjectDataWrapper}/>
            <PublicRoute path="*" component={NotFoundPage}/>
        </Switch>
    )
}
