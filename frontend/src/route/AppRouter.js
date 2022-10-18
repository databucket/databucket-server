import React from "react";
import {BrowserRouter, Redirect, Switch} from 'react-router-dom';
import LoginPage from '../components/login/LoginPage';
import NotFoundPage from '../components/NotFoundPage';
import PublicRoute from './PublicRoute'
import ProjectRoute from './ProjectRoute'
import ManagementRoute from './ManagementRoute';
import _ManagementTabs from "../components/management/_ManagementTabs";
import ChangePasswordPage from "../components/login/ChangePasswordPage";
import {getActiveProjectId, hasProject, setPathname} from "../utils/ConfigurationStorage";
import _ProjectRouteInternal from "../components/data/_ProjectRouteInternal";
import ChangePasswordRoute from "./ChangePasswordRoute";
import {getContextPath} from "../utils/UrlBuilder";

export default function AppRouter() {

    const fullPathname = window.location.pathname;
    const pathname = fullPathname.replace(getContextPath(), "");

    console.log("----save pathname -------------------------------------------");
    console.log("contextPath: " + getContextPath());
    console.log("fullPathname: " + fullPathname);
    console.log("pathname: " + pathname);
    console.log("-------------------------------------------------------------");

    setPathname(pathname);
    return (
        <BrowserRouter
            basename={getContextPath()}
        >
            <Switch>
                <Redirect exact from='/' to={getProjectDataPath()}/>
                <PublicRoute exact restricted={true} path="/login" component={LoginPage}/>
                <ChangePasswordRoute exact path="/change-password" component={ChangePasswordPage}/>
                <ManagementRoute path="/management" component={_ManagementTabs}/>
                <ProjectRoute path="/project" component={_ProjectRouteInternal}/>
                <PublicRoute path="*" component={NotFoundPage}/>
            </Switch>
        </BrowserRouter>
    );
}

export const getProjectDataPath = () => {
    return hasProject() ? `/project/${getActiveProjectId()}` : '/login'
}

export const getGivenProjectDataPath = (projectId) => {
    return `/project/${projectId}`;
}

export const getProjectSettingsPath = () => {
    return hasProject() ? `/project/${getActiveProjectId()}/settings` : '/login'
}

export const getManagementProjectsPath = () => {
    return '/management/projects';
}

export const getManagementUsersPath = () => {
    return '/management/users';
}

export const getManagementTemplatesPath = () => {
    return '/management/templates';
}

export const getManagementDataPath = () => {
    return '/management/data';
}

export const getManagementLogsPath = () => {
    return '/management/logs';
}

export const getDirectDataPath = (bucket, dataId) => {
    return `${window.location.origin}${getContextPath()}/project/${getActiveProjectId()}/bucket/${bucket}/data/${dataId}`;
}
