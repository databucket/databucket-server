import React, {useState} from 'react';
import {makeStyles} from "@material-ui/core/styles";
import AppBar from "@material-ui/core/AppBar";
import Toolbar from "@material-ui/core/Toolbar";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import IconButton from "@material-ui/core/IconButton";
import CloseIcon from "@material-ui/icons/Close";
import {Link, Redirect, Route, Switch} from "react-router-dom";
import {getAppBarBackgroundColor} from "../../utils/Themes";
import {getManagementProjectsPath, getManagementTemplatesPath, getManagementUsersPath, getProjectDataPath} from "../../route/AppRouter";
import {getLastManagementPageName, hasProject, hasToken, logOut, setLastManagementPageName} from "../../utils/ConfigurationStorage";
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

const useStyles = makeStyles(theme => ({
    appBar: {
        position: 'relative',
        background: getAppBarBackgroundColor()
    },
    title: {
        marginLeft: theme.spacing(2),
    },
    tabs: {
        flex: 1,
    },
}));

export default function _ManagementTabs() {

    document.title = 'Databucket';

    const classes = useStyles();
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
        return (
            <Route
                path="/"
                render={({location}) => (
                    <div>
                        <AppBar className={classes.appBar}>
                            <Toolbar variant={'dense'}>
                                {hasProject() ? (
                                    <IconButton color="inherit" edge="start" component={Link} to={getProjectDataPath()} aria-label="Close">
                                        <CloseIcon/>
                                    </IconButton>
                                ) : (<div/>)}

                                <Tabs value={getTabsValue(location.pathname)}
                                      variant="scrollable"
                                      scrollButtons="on"
                                      className={classes.tabs}>
                                    <Tab label="Projects" value={tabs[0]} component={Link} to={getManagementProjectsPath()}/>
                                    <Tab label="Users" value={tabs[1]} component={Link} to={getManagementUsersPath()}/>
                                    <Tab label="Templates" value={tabs[2]} component={Link} to={getManagementTemplatesPath()}/>
                                </Tabs>
                                <div/>
                                <UserProfile onLogout={handleLogout}/>
                            </Toolbar>
                        </AppBar>
                        <ProjectsProvider> <ManageUsersProvider> <RolesProvider> <TemplatesProvider> <DataProvider> <DataItemsProvider>
                            <Switch>
                                <ManagementRoute exact path={getManagementProjectsPath()} component={ProjectsTab}/>
                                <ManagementRoute exact path={getManagementUsersPath()} component={UsersTab}/>
                                <ManagementRoute exact path={getManagementTemplatesPath()} component={TemplatesTab}/>
                                <PublicRoute path="*" component={NotFoundPage}/>
                            </Switch>
                        </DataItemsProvider> </DataProvider> </TemplatesProvider> </RolesProvider> </ManageUsersProvider> </ProjectsProvider>
                    </div>
                )}
            />
        );
    } else
        return (<Redirect to="/login"/>);

}
