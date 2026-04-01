import React from 'react';
import { Navigate } from 'react-router-dom';
import { hasToken } from '../utils/ConfigurationStorage';

const ChangePasswordRoute = ({ children }) => {
    if (!hasToken()) {
        return <Navigate to="/login" replace />;
    }

    return children;
}

export default ChangePasswordRoute;
