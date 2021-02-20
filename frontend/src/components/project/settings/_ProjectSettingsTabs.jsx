import React from 'react';
import {Link, Route, Switch} from "react-router-dom";
import {Tab, Tabs} from "@material-ui/core";
import {getProjectDataPath, getProjectSettingsPath} from "../../../route/AppRouter";
import {makeStyles} from "@material-ui/core/styles";
import {getAppBarBackgroundColor} from "../../../utils/Themes";
import AppBar from "@material-ui/core/AppBar";
import Toolbar from "@material-ui/core/Toolbar";
import IconButton from "@material-ui/core/IconButton";
import CloseIcon from "@material-ui/icons/Close";
import {getLastSettingsPageName, setLastSettingsPageName} from "../../../utils/ConfigurationStorage";
import ProjectRoute from "../../../route/ProjectRoute";
import NotFoundPage from "../../NotFoundPage";
import GroupsTab from "./GroupsTab";
import ClassesTab from "./ClassesTab";
import BucketsTab from "./BucketsTab";
import BucketsProvider from "../../../context/buckets/BucketsProvider";
import GroupsProvider from "../../../context/groups/GroupsProvider";
import RolesProvider from "../../../context/roles/RolesProvider";
import UsersProvider from "../../../context/users/UsersProvider";
import ClassesProvider from "../../../context/classes/ClassesProvider";
import UsersTab from "./UsersTab";
import TagsTab from "./TagsTab";
import TagsProvider from "../../../context/tags/TagsProvider";
import ColumnsProvider from "../../../context/columns/ColumnsProvider";
import ColumnsTab from "./ColumnsTab";
import EnumsTab from "./EnumsTab";
import EnumsProvider from "../../../context/enums/EnumsProvider";
import FiltersProvider from "../../../context/filters/FiltersProvider";
import FiltersTab from "./FiltersTab";

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


export default function _ProjectSettingsTabs() {

    const classes = useStyles();
    const tabs = ['users', 'buckets', 'groups', 'classes', 'tags', 'columns', 'enums', 'filters', 'views', 'tasks', 'events', 'logs'];

    const getTabsValue = (pathname) => {
        let value = pathname.split("/").pop();
        let tabName = tabs[0];

        if (tabs.includes(value)) {
            setLastSettingsPageName(value);
            tabName = value;
        } else if (tabs.includes(getLastSettingsPageName())) {
            tabName = getLastSettingsPageName();
        }
        return tabName;
    }

    return (
        <Route
            path="/"
            render={({location}) => (
                <div>
                    <AppBar className={classes.appBar}>
                        <Toolbar variant="dense">
                            <IconButton color="inherit" edge="start" component={Link} to={getProjectDataPath()}
                                        aria-label="Close">
                                <CloseIcon/>
                            </IconButton>
                            <Tabs value={getTabsValue(location.pathname)}
                                  variant="scrollable"
                                  scrollButtons="on"
                                  className={classes.tabs}>
                                <Tab label="Users" value={tabs[0]} component={Link}
                                     to={`${getProjectSettingsPath()}/users`}/>
                                <Tab label="Buckets" value={tabs[1]} component={Link}
                                     to={`${getProjectSettingsPath()}/buckets`}/>
                                <Tab label="Groups" value={tabs[2]} component={Link}
                                     to={`${getProjectSettingsPath()}/groups`}/>
                                <Tab label="Classes" value={tabs[3]} component={Link}
                                     to={`${getProjectSettingsPath()}/classes`}/>
                                <Tab label="Tags" value={tabs[4]} component={Link}
                                     to={`${getProjectSettingsPath()}/tags`}/>
                                <Tab label="Columns" value={tabs[5]} component={Link}
                                     to={`${getProjectSettingsPath()}/columns`}/>
                                <Tab label="Enums" value={tabs[6]} component={Link}
                                     to={`${getProjectSettingsPath()}/enums`}/>
                                <Tab label="Filters" value={tabs[7]} component={Link}
                                     to={`${getProjectSettingsPath()}/filters`}/>
                                <Tab label="Views" value={tabs[8]} component={Link}
                                     to={`${getProjectSettingsPath()}/views`}/>
                                <Tab label="Tasks" value={tabs[9]} component={Link}
                                     to={`${getProjectSettingsPath()}/tasks`}/>
                                <Tab label="Events" value={tabs[10]} component={Link}
                                     to={`${getProjectSettingsPath()}/events`}/>
                                <Tab label="Logs" value={tabs[11]} component={Link}
                                     to={`${getProjectSettingsPath()}/logs`}/>
                            </Tabs>
                        </Toolbar>
                    </AppBar>
                    <RolesProvider> <GroupsProvider> <BucketsProvider> <UsersProvider>
                        <ClassesProvider> <TagsProvider> <ColumnsProvider> <EnumsProvider>
                            <FiltersProvider>
                                <Switch>
                                    <ProjectRoute
                                        exact
                                        path={`${getProjectSettingsPath()}/users`}
                                        component={UsersTab}
                                    />
                                    <ProjectRoute
                                        exact
                                        path={`${getProjectSettingsPath()}/buckets`}
                                        component={BucketsTab}
                                    />
                                    <ProjectRoute
                                        exact
                                        path={`${getProjectSettingsPath()}/groups`}
                                        component={GroupsTab}
                                    />
                                    <ProjectRoute
                                        exact
                                        path={`${getProjectSettingsPath()}/classes`}
                                        component={ClassesTab}
                                    />
                                    <ProjectRoute
                                        exact
                                        path={`${getProjectSettingsPath()}/tags`}
                                        component={TagsTab}
                                    />
                                    <ProjectRoute
                                        exact
                                        path={`${getProjectSettingsPath()}/columns`}
                                        component={ColumnsTab}
                                    />
                                    <ProjectRoute
                                        exact
                                        path={`${getProjectSettingsPath()}/enums`}
                                        component={EnumsTab}
                                    />
                                    <ProjectRoute
                                        exact
                                        path={`${getProjectSettingsPath()}/filters`}
                                        component={FiltersTab}
                                    />
                                    <ProjectRoute
                                        exact
                                        path={`${getProjectSettingsPath()}/views`}
                                        component={() => <div>Views</div>}
                                    />
                                    <ProjectRoute
                                        exact
                                        path={`${getProjectSettingsPath()}/tasks`}
                                        component={() => <div>Tasks</div>}
                                    />
                                    <ProjectRoute
                                        exact
                                        path={`${getProjectSettingsPath()}/events`}
                                        component={() => <div>Events</div>}
                                    />
                                    <ProjectRoute
                                        exact
                                        path={`${getProjectSettingsPath()}/logs`}
                                        component={() => <div>Logs</div>}
                                    />
                                    <ProjectRoute
                                        path={`${getProjectSettingsPath()}/*`}
                                        component={NotFoundPage}
                                    />
                                </Switch>
                            </FiltersProvider>
                        </EnumsProvider> </ColumnsProvider> </TagsProvider> </ClassesProvider>
                    </UsersProvider> </BucketsProvider> </GroupsProvider> </RolesProvider>
                </div>
            )}
        />
    );
}
