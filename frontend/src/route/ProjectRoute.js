import React from 'react';
import { Route, Redirect } from 'react-router-dom';
import { hasToken, hasProject } from '../utils/ConfigurationStorage';

const ProjectRoute = ({ component: Component, ...rest }) => (
    <Route {...rest} render={props => (hasToken() && hasProject() ? <Component {...props} /> : <Redirect to="/login-form" />)} />
)
export default ProjectRoute;

