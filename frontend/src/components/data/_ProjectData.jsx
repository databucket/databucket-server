import React, {useContext, useEffect, useState} from 'react';
import {styled, useTheme} from '@mui/material/styles';
import MuiDrawer from '@mui/material/Drawer';
import Toolbar from '@mui/material/Toolbar';
import List from '@mui/material/List';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import MenuIcon from '@mui/icons-material/Menu';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import UserProfile from "./UserProfile";
import {
    getLastManagementPageName,
    getLastSettingsPageName,
    hasAdminRole,
    hasProject,
    hasRobotRole,
    hasSuperRole,
    hasToken,
    isLeftPanelOpen,
    logOut,
    setActiveProjectId,
    setLeftPanelOpen,
    setPathname,
    setToken
} from "../../utils/ConfigurationStorage";
import {Link, Redirect} from 'react-router-dom';
import BucketDataTable from "./BucketDataTable";
import {getProjectSettingsPath} from "../../route/AppRouter";
import GroupMenuSelector from "./GroupMenuSelector";
import BucketListSelector from "./BucketListSelector";
import InfoDialog from "../dialogs/InfoDialog";
import BucketTabSelector from "./BucketTabSelector";
import UserProjects from "./UserProjects";
import {MessageBox} from "../utils/MessageBox";
import {handleErrors} from "../../utils/FetchHelper";
import {getButtonColor, getPostOptions} from "../../utils/MaterialTableHelper";
import {getBaseUrl, getSwaggerDocPath} from "../../utils/UrlBuilder";
import AccessContext from "../../context/access/AccessContext";
import {CenteredWaitingCircularProgress} from "../utils/CenteredWaitingCircularProgress";
import {CustomAppBar, drawerWidth} from "../common/CustomAppBar";
import {Box, ListItemButton} from "@mui/material";

const openedMixin = (theme) => ({
    width: drawerWidth,
    transition: theme.transitions.create('width', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.enteringScreen,
    }),
    overflowX: 'hidden',
});

const closedMixin = (theme) => ({
    transition: theme.transitions.create('width', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
    }),
    overflowX: 'hidden',
    width: `calc(${theme.spacing(9)} + 1px)`,
});

const Main = styled('main', {shouldForwardProp: (prop) => prop !== 'open'})(
    ({theme, open}) => ({
        flexGrow: 1,
        padding: theme.spacing(0),
        transition: theme.transitions.create('margin', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
        marginLeft: 0,
        ...(open && {
            transition: theme.transitions.create('margin', {
                easing: theme.transitions.easing.easeOut,
                duration: theme.transitions.duration.enteringScreen,
            }),
        }),
    }),
);

const Drawer = styled(MuiDrawer, {shouldForwardProp: (prop) => prop !== 'open'})(
    ({theme, open}) => ({
        width: drawerWidth,
        flexShrink: 0,
        whiteSpace: 'nowrap',
        boxSizing: 'border-box',
        ...(open && {
            ...openedMixin(theme),
            '& .MuiDrawer-paper': openedMixin(theme),
        }),
        ...(!open && {
            ...closedMixin(theme),
            '& .MuiDrawer-paper': closedMixin(theme),
        }),
    }),
);

const DrawerHeader = styled('div')(({theme}) => ({
    display: 'flex',
    alignItems: 'center',
    margin: theme.spacing(0.5, 0),
    padding: theme.spacing(0, 1),
    // necessary for content to be below app bar
    ...theme.mixins.toolbar,
    justifyContent: 'flex-end',
}));
export default function ProjectData() {

    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const theme = useTheme();
    const [open, setOpen] = useState(isLeftPanelOpen());
    const [logged, setLogged] = useState(hasToken() && hasProject());
    const accessContext = useContext(AccessContext);
    const {
        fetchAccessTree,
        projects,
        views,
        fetchSessionClasses,
        columns, fetchSessionColumns,
        filters, fetchSessionFilters,
        fetchSessionTags,
        fetchSessionSvgs,
        tasks, fetchSessionTasks,
        enums, fetchSessionEnums,
        fetchSessionUsers
    } = accessContext;

    useEffect(() => {
        fetchSessionUsers();
        fetchAccessTree();
        fetchSessionClasses();
        fetchSessionTags();
        fetchSessionSvgs();
        fetchSessionEnums();
    }, []);

    useEffect(() => {
        if (views != null && columns == null)
            fetchSessionColumns();
    }, [views]);

    useEffect(() => {
        if (views != null && tasks != null && filters == null)
            fetchSessionFilters();
    }, [views, tasks]);

    useEffect(() => {
        if (views != null) {
            if (columns == null) {
                fetchSessionColumns();
            }
            if (tasks == null) {
                fetchSessionTasks();
            }
        }
    }, [views]);

    useEffect(() => {
        if (views != null && tasks != null && filters == null)
                fetchSessionFilters();
    }, [views, tasks]);

    const handleDrawerOpen = (event) => {
        event.stopPropagation();
        setOpen(true);
        setLeftPanelOpen(true);
    };

    const handleDrawerClose = (event) => {
        event.stopPropagation();
        setOpen(false);
        setLeftPanelOpen(false);
    };

    const handleLogout = () => {
        logOut();
        setLogged(hasToken());
    }

    const onChangeProject = (projectId) => {
        const options = getPostOptions({projectId});
        fetch(getBaseUrl('users/change-project'), options)
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
            })
            .then(data => {
                setToken(data.token);
                setActiveProjectId(projectId);
                window.location.reload();
            });
    };

    if (!logged)
        return (<Redirect to="/login-form"/>);

    if (projects == null || enums == null || columns == null || filters == null || tasks == null)
        return (<CenteredWaitingCircularProgress/>);

    setPathname(null); // clear path
    return (
        <>
            <Box sx={{display: 'flex'}}>
                <CustomAppBar open={open} position="fixed">
                    <Toolbar>
                        <IconButton
                            color="inherit"
                            aria-label="open drawer"
                            onClick={handleDrawerOpen}
                            edge="start"
                            sx={{mr: 2, ...(open && {display: 'none'})}}
                            size="large">
                            <MenuIcon/>
                        </IconButton>
                        <BucketTabSelector/>
                        <UserProfile onLogout={handleLogout}/>
                        <UserProjects onChangeProject={onChangeProject}/>
                    </Toolbar>
                </CustomAppBar>
                <Drawer
                    variant="permanent"
                    open={open}
                >
                    <DrawerHeader>
                        <IconButton onClick={handleDrawerClose} size="large">
                            {theme.direction === 'rtl' ? <ChevronRightIcon/> : <ChevronLeftIcon/>}
                        </IconButton>
                    </DrawerHeader>
                    <Divider/>
                    <GroupMenuSelector open={open}/>
                    <BucketListSelector/>
                    <Divider/>
                    <List sx={{marginTop: "auto"}}>
                        {
                            hasRobotRole() && (
                                <ListItemButton target='_blank' component={Link} to={getSwaggerDocPath()}>
                                    <ListItemIcon>
                                        <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" width="24" height="24"
                                             fill={getButtonColor(theme)}>
                                            <path
                                                d="M 12 0 C 5.383 0 0 5.383 0 12 s 5.383 12 12 12 c 6.616 0 12 -5.383 12 -12 S 18.616 0 12 0 z m 0 2 c 5.995 0 10 5 10 10 c 0 5.995 -5 10 -10 10 c -5.996 0 -10 -4 -10 -10 C 2 6 6 2 12 2 z M 8.37 5.868 a 6.707 6.707 0 0 0 -0.423 0.005 c -0.983 0.056 -1.573 0.517 -1.735 1.472 c -0.115 0.665 -0.096 1.348 -0.143 2.017 c -0.013 0.35 -0.05 0.697 -0.115 1.038 c -0.134 0.609 -0.397 0.798 -1.016 0.83 a 2.65 2.65 0 0 0 -0.244 0.042 v 1.463 c 1.126 0.055 1.278 0.452 1.37 1.629 c 0.033 0.429 -0.013 0.858 0.015 1.287 c 0.018 0.406 0.073 0.808 0.156 1.2 c 0.259 1.075 1.307 1.435 2.575 1.218 v -1.283 c -0.203 0 -0.383 0.005 -0.558 0 c -0.43 -0.013 -0.591 -0.12 -0.632 -0.535 c -0.056 -0.535 -0.042 -1.08 -0.075 -1.62 c -0.064 -1.001 -0.175 -1.988 -1.153 -2.625 c 0.503 -0.37 0.868 -0.812 0.983 -1.398 c 0.083 -0.41 0.134 -0.821 0.166 -1.237 c 0.028 -0.415 -0.023 -0.84 0.014 -1.25 c 0.06 -0.665 0.102 -0.937 0.9 -0.91 c 0.12 0 0.235 -0.017 0.369 -0.027 v -1.31 c -0.16 0 -0.31 -0.004 -0.454 -0.006 z m 7.593 0.009 a 4.247 4.247 0 0 0 -0.813 0.06 v 1.274 c 0.245 0 0.434 0 0.623 0.005 c 0.328 0.004 0.577 0.13 0.61 0.494 c 0.032 0.332 0.031 0.669 0.064 1.006 c 0.065 0.669 0.101 1.347 0.217 2.007 c 0.102 0.544 0.475 0.95 0.941 1.283 c -0.817 0.549 -1.057 1.333 -1.098 2.215 c -0.023 0.604 -0.037 1.213 -0.069 1.822 c -0.028 0.554 -0.222 0.734 -0.78 0.748 c -0.157 0.004 -0.31 0.018 -0.484 0.028 v 1.305 c 0.327 0 0.627 0.019 0.927 0 c 0.932 -0.055 1.495 -0.507 1.68 -1.412 c 0.078 -0.498 0.124 -1 0.138 -1.504 c 0.032 -0.461 0.028 -0.927 0.074 -1.384 c 0.069 -0.715 0.397 -1.01 1.112 -1.057 a 0.972 0.972 0 0 0 0.199 -0.046 v -1.463 c -0.12 -0.014 -0.204 -0.027 -0.291 -0.032 c -0.536 -0.023 -0.804 -0.203 -0.937 -0.71 a 5.146 5.146 0 0 1 -0.152 -0.993 c -0.037 -0.618 -0.033 -1.241 -0.074 -1.86 c -0.08 -1.192 -0.794 -1.753 -1.887 -1.786 z m -6.89 5.28 a 0.844 0.844 0 0 0 -0.083 1.684 h 0.055 a 0.83 0.83 0 0 0 0.877 -0.78 v -0.046 a 0.845 0.845 0 0 0 -0.83 -0.858 z m 2.911 0 a 0.808 0.808 0 0 0 -0.834 0.78 c 0 0.027 0 0.05 0.004 0.078 c 0 0.503 0.342 0.826 0.859 0.826 c 0.507 0 0.826 -0.332 0.826 -0.853 c -0.005 -0.503 -0.342 -0.836 -0.855 -0.831 z m 2.963 0 a 0.861 0.861 0 0 0 -0.876 0.835 c 0 0.47 0.378 0.849 0.849 0.849 h 0.009 c 0.425 0.074 0.853 -0.337 0.881 -0.83 c 0.023 -0.457 -0.392 -0.854 -0.863 -0.854 z"/>
                                        </svg>
                                    </ListItemIcon>
                                    <ListItemText primary={'Swagger'}
                                                  primaryTypographyProps={{style: {color: theme.palette.text.primary}}}/>
                                </ListItemButton>
                            )
                        }
                        {
                            hasAdminRole() && (
                                <ListItemButton component={Link}
                                                to={getProjectSettingsPath() + "/" + getLastSettingsPageName()}>
                                    <ListItemIcon>
                                        <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" width="24" height="24"
                                             fill={getButtonColor(theme)}>
                                            <path
                                                d="M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.74,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.07,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z"/>
                                        </svg>
                                    </ListItemIcon>
                                    <ListItemText primary={'Settings'}
                                                  primaryTypographyProps={{style: {color: theme.palette.text.primary}}}/>
                                </ListItemButton>
                            )
                        }
                        {
                            hasSuperRole() && (
                                <ListItemButton component={Link} to={"/management/" + getLastManagementPageName()}>
                                    <ListItemIcon>
                                        <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" width="24" height="24"
                                             fill={getButtonColor(theme)}>
                                            <circle cx="10" cy="8" r="4"/>
                                            <path
                                                d="M10.67,13.02C10.45,13.01,10.23,13,10,13c-2.42,0-4.68,0.67-6.61,1.82C2.51,15.34,2,16.32,2,17.35V20h9.26 C10.47,18.87,10,17.49,10,16C10,14.93,10.25,13.93,10.67,13.02z"/>
                                            <path
                                                d="M20.75,16c0-0.22-0.03-0.42-0.06-0.63l1.14-1.01l-1-1.73l-1.45,0.49c-0.32-0.27-0.68-0.48-1.08-0.63L18,11h-2l-0.3,1.49 c-0.4,0.15-0.76,0.36-1.08,0.63l-1.45-0.49l-1,1.73l1.14,1.01c-0.03,0.21-0.06,0.41-0.06,0.63s0.03,0.42,0.06,0.63l-1.14,1.01 l1,1.73l1.45-0.49c0.32,0.27,0.68,0.48,1.08,0.63L16,21h2l0.3-1.49c0.4-0.15,0.76-0.36,1.08-0.63l1.45,0.49l1-1.73l-1.14-1.01 C20.72,16.42,20.75,16.22,20.75,16z M17,18c-1.1,0-2-0.9-2-2s0.9-2,2-2s2,0.9,2,2S18.1,18,17,18z"/>
                                        </svg>
                                    </ListItemIcon>
                                    <ListItemText primary={'Management'}
                                                  primaryTypographyProps={{style: {color: theme.palette.text.primary}}}/>
                                </ListItemButton>
                            )
                        }
                        <InfoDialog/>
                    </List>
                </Drawer>
                <Main open={open}>
                    <DrawerHeader/>
                    <BucketDataTable/>
                </Main>
            </Box>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </>
    );
}
