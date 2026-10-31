import React, {useEffect, useState} from 'react';
import {Link, useParams} from "react-router-dom";
import {Button, styled, Typography} from "@mui/material";
import AccessProvider from "../../../context/access/AccessProvider";
import DataDetailsPage from "./DataDetailsPage";
import TagsProvider from "../../../context/tags/TagsProvider";
import {getActiveProjectId, setActiveProjectId, setPathname, setToken} from "../../../utils/ConfigurationStorage";
import {getPostOptions} from "../../../utils/MaterialTableHelper";
import {handleErrors} from "../../../utils/FetchHelper";
import {getBaseUrl} from "../../../utils/UrlBuilder";
import {getProjectDataPath} from "../../../route/AppRouter";
import {CenteredWaitingCircularProgress} from "../../utils/CenteredWaitingCircularProgress";

const StyledTypography = styled(Typography)(({theme}) => ({
    padding: theme.spacing(3)
}));

// The server reads the data from the active project, so a link to the data of another project switches to that
// project first (the server checks that the user belongs to it). Otherwise the link would show the row with the
// same bucket name and id in the active project.
export default function DataDetailsPageWrapper() {

    const linkProjectId = parseInt(useParams().projectId, 10);
    const [project, setProject] = useState(linkProjectId === getActiveProjectId() ? 'active' : 'switching');

    useEffect(() => {
        if (project !== 'switching')
            return;
        fetch(getBaseUrl('users/change-project'), getPostOptions({projectId: linkProjectId}))
            .then(handleErrors)
            .then(data => {
                setToken(data.token);
                setActiveProjectId(linkProjectId);
                setProject('active');
            })
            .catch(() => setProject('denied'));
    }, [project, linkProjectId]);

    if (project === 'switching')
        return <CenteredWaitingCircularProgress/>;

    if (project === 'denied') {
        setPathname(null);
        return (
            <>
                <StyledTypography variant="h6" color={'error'}>
                    {'You do not have access to the project of this link.'}
                </StyledTypography>
                <Button component={Link} to={getProjectDataPath()} sx={{ml: 2}}>Go to data</Button>
            </>
        );
    }

    return (
        <AccessProvider>
            <TagsProvider>
                <DataDetailsPage/>
            </TagsProvider>
        </AccessProvider>
    );

}
