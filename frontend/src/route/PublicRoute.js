import React from 'react';
import { Route, Redirect } from 'react-router-dom';
import { isLogin } from '../utils/ConfigurationStorage';

const PublicRoute = ({ component: Component, restricted, ...rest }) => (
    <Route {...rest} render={props => (isLogin() && restricted ? <Redirect to="/project/" /> : <Component {...props} />)} />
)

export default PublicRoute;