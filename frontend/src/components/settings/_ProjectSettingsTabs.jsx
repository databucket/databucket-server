import React from 'react';
import {styled} from '@mui/material/styles';
import {Link, Route, Switch} from "react-router-dom";
import {Tab, Tabs} from "@mui/material";
import {getProjectDataPath, getProjectSettingsPath} from "../../route/AppRouter";
import IconButton from "@mui/material/IconButton";
import CloseIcon from "@mui/icons-material/Cancel";
import {getLastSettingsPageName, setLastSettingsPageName, setPathname} from "../../utils/ConfigurationStorage";
import ProjectRoute from "../../route/ProjectRoute";
import NotFoundPage from "../NotFoundPage";
import GroupsTab from "./GroupsTab";
import ClassesTab from "./ClassesTab";
import BucketsTab from "./BucketsTab";
import TagsTab from "./TagsTab";
import ColumnsTab from "./ColumnsTab";
import EnumsTab from "./EnumsTab";
import FiltersTab from "./FiltersTab";
import ViewsTab from "./ViewsTab";
import TasksTab from "./TasksTab";
import {getSettingsTabsBackgroundColor, getSettingsTabsColor} from "../../utils/MaterialTableHelper";
import TemplatesTab from "../settings/TemplatesTab";
import SvgTab from "./SvgTab";
import RolesProvider from "../../context/roles/RolesProvider";
import GroupsProvider from "../../context/groups/GroupsProvider";
import BucketsProvider from "../../context/buckets/BucketsProvider";
import UsersProvider from "../../context/users/UsersProvider";
import ClassesProvider from "../../context/classes/ClassesProvider";
import TagsProvider from "../../context/tags/TagsProvider";
import ColumnsProvider from "../../context/columns/ColumnsProvider";
import EnumsProvider from "../../context/enums/EnumsProvider";
import FiltersProvider from "../../context/filters/FiltersProvider";
import TasksProvider from "../../context/tasks/TasksProvider";
import EventsProvider from "../../context/events/EventsProvider";
import ViewsProvider from "../../context/views/ViewsProvider";
import TeamsProvider from "../../context/teams/TeamsProvider";
import TemplatesProvider from "../../context/templates/TemplatesProvider";
import SvgProvider from "../../context/svgs/SvgProvider";
import TeamsTab from "./TeamsTab";
import UsersTab from "./UsersTab";

const PREFIX = '_ProjectSettingsTabs';

const classes = {
    selected: `${PREFIX}-selected`,
    tabs: `${PREFIX}-tabs`,
    panel: `${PREFIX}-panel`
};

const Container = styled('div')(({theme}) => ({
    flexGrow: 1,
    display: 'flex',

    [`& .${classes.tabs}`]: {
        color: getSettingsTabsColor(theme),
        backgroundColor: getSettingsTabsBackgroundColor(theme),
        borderRight: `1px solid ${theme.palette.divider}`,
        width: '8%'
    },

    [`& .${classes.panel}`]: {
        width: '92%'
    }
}));

const StyledTab = Tab

export default function _ProjectSettingsTabs() {

    const tabs = ['teams', 'users', 'classes', 'icons', 'enums', 'groups', 'buckets', 'tags', 'columns', 'filters', 'views', 'tasks', 'templates'];

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

    setPathname(null); // clear path

    return (
        <Route
            path="/"
            render={({location}) => (
                <Container>
                    <div className={classes.tabs}>
                        <IconButton
                            component={Link}
                            to={getProjectDataPath()}
                            aria-label="Close"
                            size="large">
                            <CloseIcon/>
                        </IconButton>
                        <Tabs
                            value={getTabsValue(location.pathname)}
                            variant="scrollable"
                            scrollButtons
                            orientation={'vertical'}
                            allowScrollButtonsMobile>
                            <StyledTab label="Teams" value={tabs[0]} component={Link}
                                       to={`${getProjectSettingsPath()}/teams`}/>
                            <StyledTab label="Users" value={tabs[1]} component={Link}
                                       to={`${getProjectSettingsPath()}/users`}/>
                            <StyledTab label="Classes" value={tabs[2]} component={Link}
                                       to={`${getProjectSettingsPath()}/classes`}/>
                            <StyledTab label="Icons" value={tabs[3]} component={Link}
                                       to={`${getProjectSettingsPath()}/icons`}/>
                            <StyledTab label="Enums" value={tabs[4]} component={Link}
                                       to={`${getProjectSettingsPath()}/enums`}/>
                            <StyledTab label="Groups" value={tabs[5]} component={Link}
                                       to={`${getProjectSettingsPath()}/groups`}/>
                            <StyledTab label="Buckets" value={tabs[6]} component={Link}
                                       to={`${getProjectSettingsPath()}/buckets`}/>
                            <StyledTab label="Tags" value={tabs[7]} component={Link}
                                       to={`${getProjectSettingsPath()}/tags`}/>
                            <StyledTab label="Columns" value={tabs[8]} component={Link}
                                       to={`${getProjectSettingsPath()}/columns`}/>
                            <StyledTab label="Filters" value={tabs[9]} component={Link}
                                       to={`${getProjectSettingsPath()}/filters`}/>
                            <StyledTab label="Views" value={tabs[10]} component={Link}
                                       to={`${getProjectSettingsPath()}/views`}/>
                            <StyledTab label="Tasks" value={tabs[11]} component={Link}
                                       to={`${getProjectSettingsPath()}/tasks`}/>
                            <StyledTab label="Templates" value={tabs[12]} component={Link}
                                       to={`${getProjectSettingsPath()}/templates`}/>
                        </Tabs>
                    </div>
                    <div className={classes.panel}>
                        <RolesProvider><GroupsProvider><BucketsProvider><UsersProvider><ClassesProvider>
                            <TagsProvider><ColumnsProvider> <EnumsProvider> <FiltersProvider> <TasksProvider>
                                <EventsProvider><ViewsProvider> <TeamsProvider><TemplatesProvider> <SvgProvider>
                                    <Switch>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/teams`}
                                                      component={TeamsTab}/>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/users`}
                                                      component={UsersTab}/>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/classes`}
                                                      component={ClassesTab}/>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/icons`}
                                                      component={SvgTab}/>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/enums`}
                                                      component={EnumsTab}/>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/groups`}
                                                      component={GroupsTab}/>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/buckets`}
                                                      component={BucketsTab}/>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/tags`}
                                                      component={TagsTab}/>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/columns`}
                                                      component={ColumnsTab}/>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/filters`}
                                                      component={FiltersTab}/>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/views`}
                                                      component={ViewsTab}/>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/tasks`}
                                                      component={TasksTab}/>
                                        <ProjectRoute exact
                                                      path={`${getProjectSettingsPath()}/templates`}
                                                      component={TemplatesTab}/>
                                        <ProjectRoute path={`${getProjectSettingsPath()}/*`}
                                                      component={NotFoundPage}/>
                                    </Switch>
                                </SvgProvider>
                                </TemplatesProvider>
                                </TeamsProvider> </ViewsProvider> </EventsProvider>
                            </TasksProvider> </FiltersProvider> </EnumsProvider>
                            </ColumnsProvider> </TagsProvider> </ClassesProvider> </UsersProvider>
                        </BucketsProvider> </GroupsProvider> </RolesProvider>
                    </div>
                </Container>
            )}
        />
    );
}
