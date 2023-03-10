import React from 'react';
import {Route, Redirect} from 'react-router-dom';
import {hasToken} from '../utils/ConfigurationStorage';

const ChangePasswordRoute = ({component: Component, ...rest}) => (
    <Route {...rest} render={props => (hasToken() ? <Component {...props} /> : <Redirect to="/login-form"/>)}/>
)
export default ChangePasswordRoute;

