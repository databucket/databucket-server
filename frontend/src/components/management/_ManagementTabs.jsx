import React, {useState} from 'react';
import {styled} from '@mui/material/styles';
import AppBar from "@mui/material/AppBar";
import Toolbar from "@mui/material/Toolbar";
import Tabs from "@mui/material/Tabs";
import Tab from "@mui/material/Tab";
import IconButton from "@mui/material/IconButton";
import CloseIcon from "@mui/icons-material/Close";
import {Link, Redirect, Route, Switch} from "react-router-dom";
import {getAppBarBackgroundColor} from "../../utils/Themes";
import {
    getManagementProjectsPath,
    getManagementTemplatesPath,
    getManagementUsersPath,
    getProjectDataPath
} from "../../route/AppRouter";
import {
    getLastManagementPageName,
    hasProject,
    hasToken,
    logOut,
    setLastManagementPageName,
    setPathname
} from "../../utils/ConfigurationStorage";
import ProjectsTab from "./ProjectsTab";
import UsersTab from "./UsersTab";
import NotFoundPage from "../NotFoundPage";
import ManagementRoute from "../../route/ManagementRoute";
import ProjectsProvider from "../../context/projects/ProjectsProvider";
import ManageUsersProvider from "../../context/users/ManageUsersProvider";
import RolesProvider from "../../context/roles/RolesProvider";
import UserProfile from "../data/UserProfile";
import TemplatesTab from "./TemplatesTab";
import TemplatesProvider from "../../context/templates/TemplatesProvider";
import DataItemsProvider from "../../context/templatesDataItems/DataItemsProvider";
import DataProvider from "../../context/templatesData/DataProvider";
import PublicRoute from "../../route/PublicRoute";

const PREFIX = '_ManagementTabs';

const classes = {
    appBar: `${PREFIX}-appBar`,
    title: `${PREFIX}-title`,
    tabs: `${PREFIX}-tabs`
};

const StyledRedirect = styled(Redirect)((
    {
        theme
    }
) => ({
    [`& .${classes.appBar}`]: {
        position: 'relative',
        background: getAppBarBackgroundColor()
    },

    [`& .${classes.title}`]: {
        marginLeft: theme.spacing(2),
    },

    [`& .${classes.tabs}`]: {
        flex: 1,
    }
}));

export default function _ManagementTabs() {

    document.title = 'Databucket';


    const tabs = ['projects', 'users', 'templates'];
    const [logged, setLogged] = useState(hasToken());

    const getTabsValue = (pathname) => {
        let value = pathname.split("/").pop();
        let tabName = tabs[0];

        if (tabs.includes(value)) {
            setLastManagementPageName(value);
            tabName = value;
        } else if (tabs.includes(getLastManagementPageName())) {
            tabName = getLastManagementPageName();
        }
        return tabName;
    }

    const handleLogout = () => {
        logOut();
        setLogged(hasToken());
    }

    if (logged) {
        setPathname(null); // clear path
        return (
            <Route
                path="/"
                render={({location}) => (
                    <div>
                        <AppBar className={classes.appBar}>
                            <Toolbar variant={'dense'}>
                                {hasProject() ? (
                                    <IconButton
                                        color="inherit"
                                        edge="start"
                                        component={Link}
                                        to={getProjectDataPath()}
                                        aria-label="Close"
                                        size="large">
                                        <CloseIcon/>
                                    </IconButton>
                                ) : (<div/>)}

                                <Tabs
                                    value={getTabsValue(location.pathname)}
                                    variant="scrollable"
                                    scrollButtons
                                    className={classes.tabs}
                                    allowScrollButtonsMobile>
                                    <Tab label="Projects" value={tabs[0]} component={Link}
                                         to={getManagementProjectsPath()}/>
                                    <Tab label="Users" value={tabs[1]} component={Link} to={getManagementUsersPath()}/>
                                    <Tab label="Templates" value={tabs[2]} component={Link}
                                         to={getManagementTemplatesPath()}/>
                                </Tabs>
                                <div/>
                                <UserProfile onLogout={handleLogout}/>
                            </Toolbar>
                        </AppBar>
                        <ProjectsProvider> <ManageUsersProvider> <RolesProvider> <TemplatesProvider> <DataProvider>
                            <DataItemsProvider>
                                <Switch>
                                    <ManagementRoute exact path={getManagementProjectsPath()} component={ProjectsTab}/>
                                    <ManagementRoute exact path={getManagementUsersPath()} component={UsersTab}/>
                                    <ManagementRoute exact path={getManagementTemplatesPath()}
                                                     component={TemplatesTab}/>
                                    <PublicRoute path="*" component={NotFoundPage}/>
                                </Switch>
                            </DataItemsProvider> </DataProvider> </TemplatesProvider> </RolesProvider>
                        </ManageUsersProvider> </ProjectsProvider>
                    </div>
                )}
            />
        );
    } else
        return (<Redirect to="/login-form"/>);

}
