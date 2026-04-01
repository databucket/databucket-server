import React from 'react';
import { Navigate } from 'react-router-dom';
import { hasProject, hasToken } from '../utils/ConfigurationStorage';
import { getProjectDataPath } from "./AppRouter";

const PublicRoute = ({ children, restricted }) => {
    if (hasToken() && hasProject() && restricted) {
        return <Navigate to={getProjectDataPath()} replace />;
    }

    return children;
}

export default PublicRoute;
