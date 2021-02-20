import React from 'react';
import { Route, Redirect } from 'react-router-dom';
import {hasSuperRole, hasToken} from '../utils/ConfigurationStorage';

const ManagementRoute = ({ component: Component, ...rest }) => (
    <Route {...rest} render={props => (hasToken() && hasSuperRole() ? <Component {...props} /> : <Redirect to="/login" />)} />
)
export default ManagementRoute;

