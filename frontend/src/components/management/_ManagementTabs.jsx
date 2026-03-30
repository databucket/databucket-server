import React, {useState, Suspense, lazy} from 'react';
import {IconButton, Stack, Tabs, Toolbar} from "@mui/material";
import {Close as CloseIcon} from "@mui/icons-material";
import {Link, Redirect, Route, Switch} from "react-router-dom";
import {
    getManagementProjectsPath,
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
import NotFoundPage from "../NotFoundPage";
import ManagementRoute from "../../route/ManagementRoute";
import ProjectsProvider from "../../context/projects/ProjectsProvider";
import ManageUsersProvider from "../../context/users/ManageUsersProvider";
import RolesProvider from "../../context/roles/RolesProvider";
import UserProfile from "../data/UserProfile";
import PublicRoute from "../../route/PublicRoute";
import {CustomAppBar, CustomTabManagement} from "../common/CustomAppBar";

export default function _ManagementTabs() {

    document.title = 'Databucket';


    const tabs = ['projects', 'users'];
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
        const ProjectsTab = lazy(() => import("./ProjectsTab"));
        const UsersTab = lazy(() => import("./UsersTab"));

        setPathname(null); // clear path
        return (
            <Route
                path="/"
                render={({location}) => (
                    <Stack>
                        <CustomAppBar position={"sticky"} sx={{flex: 1}}>
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
                                    textColor="secondary"
                                    indicatorColor="secondary"
                                    sx={{flex: 1}}>
                                    <CustomTabManagement label="Projects" value={tabs[0]} component={Link} to={getManagementProjectsPath()}/>
                                    <CustomTabManagement label="Users" value={tabs[1]} component={Link} to={getManagementUsersPath()}/>
                                </Tabs>
                                <UserProfile onLogout={handleLogout}/>
                            </Toolbar>
                        </CustomAppBar>
                        <Suspense fallback={<div>Loading...</div>}>
                            <ProjectsProvider> <ManageUsersProvider> <RolesProvider>
                                <Switch>
                                    <ManagementRoute exact path={getManagementProjectsPath()} component={ProjectsTab}/>
                                    <ManagementRoute exact path={getManagementUsersPath()} component={UsersTab}/>
                                    <PublicRoute path="*" component={NotFoundPage}/>
                                </Switch>
                            </RolesProvider> </ManageUsersProvider> </ProjectsProvider>
                        </Suspense>
                    </Stack>
                )}
            />
        );
    } else
        return (<Redirect to="/login"/>);

}
