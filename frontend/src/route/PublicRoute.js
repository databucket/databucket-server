import React from 'react';
import { Route, Redirect } from 'react-router-dom';
import {hasProject, hasToken} from '../utils/ConfigurationStorage';
import {getProjectDataPath} from "./AppRouter";

const PublicRoute = ({ component: Component, restricted, ...rest }) => (
    <Route {...rest} render={props => (hasToken() && hasProject() && restricted ? <Redirect to={getProjectDataPath()} /> : <Component {...props} />)} />
)

export default PublicRoute;