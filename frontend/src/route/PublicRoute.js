import React from 'react';
import { Route, Redirect } from 'react-router-dom';
import { isLogin } from '../utils/ConfigurationStorage';
import {getProjectDataPath} from "./AppRouter";

const PublicRoute = ({ component: Component, restricted, ...rest }) => (
    <Route {...rest} render={props => (isLogin() && restricted ? <Redirect to={getProjectDataPath()} /> : <Component {...props} />)} />
)

export default PublicRoute;