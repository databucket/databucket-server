import React from 'react';
import {Navigate, Routes, Route} from "react-router-dom";
import _ProjectSettingsTabs from "../settings/_ProjectSettingsTabs";
import {getProjectSettingsPath} from "../../route/AppRouter";
import ProjectDataWrapper from "./_ProjectDataWrapper";
import DataDetailsPageWrapper from "./details_page/DataDetailsPageWrapper";
import NotFoundPage from "../NotFoundPage";

export default function _ProjectRouteInternal() {

    return (
        <Routes>
            <Route path=":projectId/settings" element={<Navigate to={getProjectSettingsPath()} replace />} />
            <Route path=":projectId/settings/*" element={<_ProjectSettingsTabs />} />
            <Route path=":projectId/bucket/:bucketName/data/:dataId/:jsonPath" element={<DataDetailsPageWrapper />} />
            <Route path=":projectId/bucket/:bucketName/data/:dataId" element={<DataDetailsPageWrapper />} />
            <Route path=":projectId" element={<ProjectDataWrapper />} />
            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    )
}
