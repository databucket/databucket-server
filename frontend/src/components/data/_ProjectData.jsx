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
import {getButtonColor, getPostOptions} from "../../utils/MaterialTableHelper";
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
        fetchSessionSvgs,
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
        fetchSessionSvgs();
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
                                    <ListItemIcon>
                                        <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill={getButtonColor(theme)}>
                                            <path d="m12 0c-6.617 0-12 5.383-12 12s5.383 12 12 12c6.616 0 12-5.383 12-12s-5.384-12-12-12zm0 1.144c5.995 0 10.856 4.86 10.856 10.856 0 5.995-4.86 10.856-10.856 10.856s-10.856-4.86-10.856-10.856c0-5.996 4.86-10.856 10.856-10.856zm-3.63 4.724a6.707 6.707 0 0 0 -.423.005c-.983.056-1.573.517-1.735 1.472-.115.665-.096 1.348-.143 2.017-.013.35-.05.697-.115 1.038-.134.609-.397.798-1.016.83a2.65 2.65 0 0 0 -.244.042v1.463c1.126.055 1.278.452 1.37 1.629.033.429-.013.858.015 1.287.018.406.073.808.156 1.2.259 1.075 1.307 1.435 2.575 1.218v-1.283c-.203 0-.383.005-.558 0-.43-.013-.591-.12-.632-.535-.056-.535-.042-1.08-.075-1.62-.064-1.001-.175-1.988-1.153-2.625.503-.37.868-.812.983-1.398.083-.41.134-.821.166-1.237.028-.415-.023-.84.014-1.25.06-.665.102-.937.9-.91.12 0 .235-.017.369-.027v-1.31c-.16 0-.31-.004-.454-.006zm7.593.009a4.247 4.247 0 0 0 -.813.06v1.274c.245 0 .434 0 .623.005.328.004.577.13.61.494.032.332.031.669.064 1.006.065.669.101 1.347.217 2.007.102.544.475.95.941 1.283-.817.549-1.057 1.333-1.098 2.215-.023.604-.037 1.213-.069 1.822-.028.554-.222.734-.78.748-.157.004-.31.018-.484.028v1.305c.327 0 .627.019.927 0 .932-.055 1.495-.507 1.68-1.412.078-.498.124-1 .138-1.504.032-.461.028-.927.074-1.384.069-.715.397-1.01 1.112-1.057a.972.972 0 0 0 .199-.046v-1.463c-.12-.014-.204-.027-.291-.032-.536-.023-.804-.203-.937-.71a5.146 5.146 0 0 1 -.152-.993c-.037-.618-.033-1.241-.074-1.86-.08-1.192-.794-1.753-1.887-1.786zm-6.89 5.28a.844.844 0 0 0 -.083 1.684h.055a.83.83 0 0 0 .877-.78v-.046a.845.845 0 0 0 -.83-.858zm2.911 0a.808.808 0 0 0 -.834.78c0 .027 0 .05.004.078 0 .503.342.826.859.826.507 0 .826-.332.826-.853-.005-.503-.342-.836-.855-.831zm2.963 0a.861.861 0 0 0 -.876.835c0 .47.378.849.849.849h.009c.425.074.853-.337.881-.83.023-.457-.392-.854-.863-.854z"/>
                                        </svg>
                                    </ListItemIcon>
                                    <ListItemText primary={'Swagger'} primaryTypographyProps={{style: {color: theme.palette.text.primary}}}/>
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
