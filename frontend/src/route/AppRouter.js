import React from "react";
import {BrowserRouter, Redirect, Switch} from 'react-router-dom';
import LoginPage from '../components/login/LoginPage';
import _ProjectSettingsTabs from '../components/settings/_ProjectSettingsTabs';
import NotFoundPage from '../components/NotFoundPage';
import PublicRoute from './PublicRoute'
import ProjectRoute from './ProjectRoute'
import ManagementRoute from './ManagementRoute';
import _ManagementTabs from "../components/management/_ManagementTabs";
import ChangePasswordPage from "../components/login/ChangePasswordPage";
import {hasProject} from "../utils/ConfigurationStorage";
import _ProjectRouteInternal from "../components/data/_ProjectRouteInternal";
import ChangePasswordRoute from "./ChangePasswordRoute";
import {getContextPath} from "../utils/UrlBuilder";

export default function AppRouter() {

    return (
        <BrowserRouter
            basename={getContextPath()}
        >
            <Switch>
                <Redirect exact from='/' to={getProjectDataPath()}/>
                <PublicRoute exact restricted={true} path="/login" component={LoginPage}/>
                <ProjectRoute path="/project/settings" component={_ProjectSettingsTabs}/>
                <ChangePasswordRoute exact path="/change-password" component={ChangePasswordPage}/>
                <ManagementRoute path="/management" component={_ManagementTabs}/>
                <ProjectRoute path="/project" component={_ProjectRouteInternal}/>
                <PublicRoute path="*" component={NotFoundPage}/>
            </Switch>
        </BrowserRouter>
    );
}

export const getProjectDataPath = () => {
    return hasProject() ? `/project` : '/login'
}

export const getProjectSettingsPath = () => {
    return hasProject() ? `/project/settings` : '/login'
}

export const getManagementProjectsPath = () => {
    return '/management/projects';
}

export const getManagementUsersPath = () => {
    return '/management/users';
}

export const getManagementLogsPath = () => {
    return '/management/logs';
}
