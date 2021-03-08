import React, {useState} from 'react';
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
import SettingsIcon from '@material-ui/icons/Settings';
import UserProfile from "./UserProfile";
import {
    clearActiveProjectId, clearToken,
    getLastManagementPageName,
    getLastSettingsPageName,
    hasAdminRole,
    hasProject,
    hasSuperRole,
    hasToken,
    isLeftPanelOpen,
    logOut, setActiveProjectId,
    setLeftPanelOpen, setToken
} from "../../../utils/ConfigurationStorage";
import {Link, Redirect} from 'react-router-dom';
import {getAppBarBackgroundColor} from "../../../utils/Themes";
import BucketDataTable from "./BucketDataTable";
import {getProjectSettingsPath} from "../../../route/AppRouter";
import GroupMenuSelector from "./GroupMenuSelector";
import BucketListSelector from "./BucketListSelector";
import InfoDialog from "../dialogs/InfoDialog";
import BucketTabSelector from "./BucketTabSelector";
import AccessTreeProvider from "../../../context/accessTree/AccessTreeProvider";
import UserProjects from "./UserProjects";
import {MessageBox} from "../../utils/MessageBox";
import {handleErrors} from "../../../utils/FetchHelper";
import {getBaseUrl, getPostOptions} from "../../../utils/MaterialTableHelper";

const drawerWidth = 240;

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

export default function _ProjectDataPage() {
    const classes = useStyles();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const theme = useTheme();
    const [open, setOpen] = useState(isLeftPanelOpen());
    const [logged, setLogged] = useState(hasToken() && hasProject());

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

    if (logged) {
        return (
            <AccessTreeProvider>
                <div className={classes.root}>
                    <AppBar
                        position="fixed"
                        className={clsx(classes.appBar, {
                            [classes.appBarShift]: open,
                        })}
                        style={{background: getAppBarBackgroundColor()}}
                    >
                        <Toolbar>
                            <IconButton
                                color="inherit"
                                aria-label="open drawer"
                                onClick={handleDrawerOpen}
                                edge="start"
                                className={clsx(classes.menuButton, {
                                    [classes.hide]: open,
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
                        variant="permanent"
                        className={clsx(classes.drawer, {
                            [classes.drawerOpen]: open,
                            [classes.drawerClose]: !open,
                        })}
                        classes={{
                            paper: clsx({
                                [classes.drawerOpen]: open,
                                [classes.drawerClose]: !open,
                            }),
                        }}
                    >
                        <div className={classes.toolbar}>
                            <IconButton onClick={handleDrawerClose}>
                                {theme.direction === 'rtl' ? <ChevronRightIcon/> : <ChevronLeftIcon/>}
                            </IconButton>
                        </div>
                        <Divider/>
                        <GroupMenuSelector open={open}/>
                        <BucketListSelector/>
                        <div className={classes.grow}/>
                        <Divider/>
                        <List>
                            <InfoDialog/>
                            {
                                hasAdminRole() ? (
                                    <ListItem button component={Link} to={getProjectSettingsPath() + "/" + getLastSettingsPageName()}>
                                        <ListItemIcon><SettingsIcon/></ListItemIcon>
                                        <ListItemText primary={'Settings'} primaryTypographyProps={{style: {color: theme.palette.text.primary}}}/>
                                    </ListItem>
                                ) : (<div/>)
                            }
                            {
                                hasSuperRole() ? (
                                    <ListItem button component={Link} to={"/management/" + getLastManagementPageName()}>
                                        <ListItemIcon><span className="material-icons">manage_accounts</span></ListItemIcon>
                                        <ListItemText primary={'Management'} primaryTypographyProps={{style: {color: theme.palette.text.primary}}}/>
                                    </ListItem>
                                ) : (<div/>)
                            }
                        </List>
                    </Drawer>
                    <main className={classes.content}>
                        <div className={classes.toolbar}/>
                        <BucketDataTable message={"hello"}/>
                    </main>
                </div>
                <MessageBox
                    config={messageBox}
                    onClose={() => setMessageBox({...messageBox, open: false})}
                />
            </AccessTreeProvider>
        );
    } else
        return (<Redirect to="/login"/>);
}
