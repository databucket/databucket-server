import React from 'react';
import { Navigate } from 'react-router-dom';
import { hasToken, hasProject } from '../utils/ConfigurationStorage';

const ProjectRoute = ({ children }) => {
    if (!hasToken() || !hasProject()) {
        return <Navigate to="/login" replace />;
    }

    return children;
}

export default ProjectRoute;
