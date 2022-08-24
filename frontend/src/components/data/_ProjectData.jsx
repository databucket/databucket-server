import React, {useContext, useEffect, useRef, useState} from 'react';
import clsx from 'clsx';
import {makeStyles, useTheme} from '@material-ui/core/styles';
import Drawer from '@material-ui/core/Drawer';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import List from '@material-ui/core/List';
import Divider from '@material-ui/core/Divider';
import IconButton from '@material-ui/core/IconButton';
import MenuIcon from '@material-ui/icons/Menu';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import UserProfile from "./UserProfile";
import {
    clearActiveProjectId, clearToken,
    getLastManagementPageName,
    getLastSettingsPageName,
    hasAdminRole,
    hasProject, hasRobotRole,
    hasSuperRole,
    hasToken,
    isLeftPanelOpen,
    logOut, setActiveProjectId,
    setLeftPanelOpen, setToken
} from "../../utils/ConfigurationStorage";
import {Link, Redirect} from 'react-router-dom';
import {getAppBarBackgroundColor} from "../../utils/Themes";
import BucketDataTable from "./BucketDataTable";
import {getProjectSettingsPath} from "../../route/AppRouter";
import GroupMenuSelector from "./GroupMenuSelector";
import BucketListSelector from "./BucketListSelector";
import InfoDialog from "../dialogs/InfoDialog";
import BucketTabSelector from "./BucketTabSelector";
import UserProjects from "./UserProjects";
import {MessageBox} from "../utils/MessageBox";
import {handleErrors} from "../../utils/FetchHelper";
import {getPostOptions} from "../../utils/MaterialTableHelper";
import {getBaseUrl, getSwaggerDocPath} from "../../utils/UrlBuilder";
import AccessContext from "../../context/access/AccessContext";
import {CenteredWaitingCircularProgress} from "../utils/CenteredWaitingCircularProgress";

const drawerWidth = 260;

export default function ProjectData() {
    const styleClasses = useStyles();
    const drawerRef = useRef(null);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const theme = useTheme();
    const [open, setOpen] = useState(isLeftPanelOpen());
    const [logged, setLogged] = useState(hasToken() && hasProject());
    const accessContext = useContext(AccessContext);
    const [currentDrawerWidth, setCurrentDrawerWidth] = useState(0);
    const {
        fetchAccessTree,
        projects,
        views,
        fetchSessionClasses,
        columns, fetchSessionColumns,
        filters, fetchSessionFilters,
        fetchSessionTags,
        tasks, fetchSessionTasks,
        enums, fetchSessionEnums,
        fetchSessionUsers
    } = accessContext;

    useEffect(() => {
        if (open)
            setCurrentDrawerWidth(drawerWidth);
        else
            setCurrentDrawerWidth(73);
    }, [open]);

    useEffect(() => {
        fetchSessionUsers();
    }, []);

    useEffect(() => {
        fetchAccessTree();
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
        fetchSessionClasses();
    }, []);

    useEffect(() => {
        fetchSessionTags();
    }, []);

    useEffect(() => {
        fetchSessionEnums();
    }, []);

    useEffect(() => {
        if (views != null && tasks == null)
            fetchSessionTasks();
    }, [views]);

    const handleDrawerOpen = () => {
        setOpen(true);
        setLeftPanelOpen(true);
    };

    const handleDrawerClose = () => {
        setOpen(false);
        setLeftPanelOpen(false);
    };

    const handleLogout = () => {
        logOut();
        setLogged(hasToken());
    }

    const onChangeProject = (projectId) => {
        clearActiveProjectId();
        const options = getPostOptions({projectId});
        clearToken();

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
        return (<Redirect to="/login"/>);

    if (projects == null || enums == null || columns == null || filters == null || tasks == null)
        return (<CenteredWaitingCircularProgress />);

    return (
        <div>
            <div className={styleClasses.root}>
                <AppBar
                    position="fixed"
                    className={clsx(styleClasses.appBar, {
                        [styleClasses.appBarShift]: open,
                    })}
                    style={{background: getAppBarBackgroundColor()}}
                >
                    <Toolbar>
                        <IconButton
                            color="inherit"
                            aria-label="open drawer"
                            onClick={handleDrawerOpen}
                            edge="start"
                            className={clsx(styleClasses.menuButton, {
                                [styleClasses.hide]: open,
                            })}
                        >
                            <MenuIcon/>
                        </IconButton>
                        <BucketTabSelector/>
                        <UserProfile onLogout={handleLogout}/>
                        <UserProjects onChangeProject={onChangeProject}/>
                    </Toolbar>
                </AppBar>
                <Drawer
                    ref={drawerRef}
                    variant="permanent"
                    className={clsx(styleClasses.drawer, {
                        [styleClasses.drawerOpen]: open,
                        [styleClasses.drawerClose]: !open,
                    })}
                    classes={{
                        paper: clsx({
                            [styleClasses.drawerOpen]: open,
                            [styleClasses.drawerClose]: !open,
                        }),
                    }}
                >
                    <div className={styleClasses.toolbar}>
                        <IconButton onClick={handleDrawerClose}>
                            {theme.direction === 'rtl' ? <ChevronRightIcon/> : <ChevronLeftIcon/>}
                        </IconButton>
                    </div>
                    <Divider/>
                    <GroupMenuSelector open={open}/>
                    <BucketListSelector leftPanelWidth={currentDrawerWidth}/>
                    <div className={styleClasses.grow}/>
                    <Divider/>
                    <List>
                        {
                            hasRobotRole() && (
                                <ListItem button target='_blank' component={Link} to={getSwaggerDocPath()}>
                                    <ListItemIcon><span className="material-icons">api</span></ListItemIcon>
                                    <ListItemText primary={'API'} primaryTypographyProps={{style: {color: theme.palette.text.primary}}}/>
                                </ListItem>
                            )
                        }
                        {
                            hasAdminRole() && (
                                <ListItem button component={Link} to={getProjectSettingsPath() + "/" + getLastSettingsPageName()}>
                                    <ListItemIcon><span className="material-icons">settings</span></ListItemIcon>
                                    <ListItemText primary={'Settings'} primaryTypographyProps={{style: {color: theme.palette.text.primary}}}/>
                                </ListItem>
                            )
                        }
                        {
                            hasSuperRole() && (
                                <ListItem button component={Link} to={"/management/" + getLastManagementPageName()}>
                                    <ListItemIcon><span className="material-icons">manage_accounts</span></ListItemIcon>
                                    <ListItemText primary={'Management'} primaryTypographyProps={{style: {color: theme.palette.text.primary}}}/>
                                </ListItem>
                            )
                        }
                        <InfoDialog/>
                    </List>
                </Drawer>
                <main className={styleClasses.content}>
                    <div className={styleClasses.toolbar}/>
                    <BucketDataTable leftPanelWidth={currentDrawerWidth}/>
                </main>
            </div>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}
;

const useStyles = makeStyles((theme) => ({
    root: {
        display: 'flex',
    },
    grow: {
        flexGrow: 1,
    },
    appBar: {
        zIndex: theme.zIndex.drawer + 1,
        transition: theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
    },
    appBarShift: {
        marginLeft: drawerWidth,
        width: `calc(100% - ${drawerWidth}px)`,
        transition: theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
        }),
    },
    menuButton: {
        marginRight: theme.spacing(2)
    },
    hide: {
        display: 'none',
    },
    drawer: {
        width: drawerWidth,
        flexShrink: 0,
        whiteSpace: 'nowrap',
    },
    drawerOpen: {
        width: drawerWidth,
        transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
        }),
    },
    drawerClose: {
        transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
        overflowX: 'hidden',
        width: theme.spacing(7) + 1,
        [theme.breakpoints.up('sm')]: {
            width: theme.spacing(9) + 1,
        },
    },
    toolbar: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        padding: theme.spacing(0, 1),
        // necessary for content to be below app bar
        ...theme.mixins.toolbar,
    },
    content: {
        flexGrow: 1,
        padding: theme.spacing(0),
    },
}));
