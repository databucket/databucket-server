import React from "react";
import {BrowserRouter, Redirect, Switch} from 'react-router-dom';
import LoginPage from '../components/login/LoginPage';
import ProjectPage from '../components/project/ProjectPage';
import SettingsPage from '../components/SettingsPage';
import NotFoundPage from '../components/NotFoundPage';
import PublicRoute from './PublicRoute'
import PrivateRoute from './PrivateRoute'
import ManagementRoute from './ManagementRoute';
import ManagementTabs from "../components/managementTabs/ManagementTabs";
import ChangePasswordPage from "../components/login/ChangePasswordPage";

export default function AppRouter() {
    return (
        <BrowserRouter>
            <Switch>
                <Redirect exact from='/' to='/project'/>
                <PublicRoute exact restricted={true} path="/login" component={LoginPage}/>
                <PrivateRoute exact path="/settings" component={SettingsPage}/>
                <ManagementRoute exact path="/change-password" component={ChangePasswordPage}/>
                <ManagementRoute exact path="/management" component={ManagementTabs}/>
                <PrivateRoute path="/project" component={ProjectPage}/>
                <PrivateRoute path="*" component={NotFoundPage}/>
            </Switch>
        </BrowserRouter>
    );
}
