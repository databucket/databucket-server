import React, {useState} from 'react';
import Toolbar from "@mui/material/Toolbar";
import Tabs from "@mui/material/Tabs";
import IconButton from "@mui/material/IconButton";
import CloseIcon from "@mui/icons-material/Close";
import {Link, Redirect, Route, Switch} from "react-router-dom";
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
import {CustomAppBar, CustomTab} from "../common/CustomAppBar";

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
                    <>
                        <CustomAppBar position="fixed" sx={{flex: 1}}>
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
                                    allowScrollButtonsMobile
                                    sx={{flex: 1}}>
                                    <CustomTab label="Projects" value={tabs[0]} component={Link}
                                               to={getManagementProjectsPath()}/>
                                    <CustomTab label="Users" value={tabs[1]} component={Link}
                                               to={getManagementUsersPath()}/>
                                    <CustomTab label="Templates" value={tabs[2]} component={Link}
                                               to={getManagementTemplatesPath()}/>
                                </Tabs>
                                <UserProfile onLogout={handleLogout}/>
                            </Toolbar>
                        </CustomAppBar>
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
                    </>
                )}
            />
        );
    } else
        return (<Redirect to="/login-form"/>);

}
