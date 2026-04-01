import React from 'react';
import { Navigate } from 'react-router-dom';
import { hasSuperRole, hasToken } from '../utils/ConfigurationStorage';

const ManagementRoute = ({ children }) => {
    if (!hasToken() || !hasSuperRole()) {
        return <Navigate to="/login" replace />;
    }

    return children;
}

export default ManagementRoute;
