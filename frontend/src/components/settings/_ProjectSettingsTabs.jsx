import React from 'react';
import {IconButton, styled, Tab, Tabs} from '@mui/material';
import {Link, Route, Routes, useLocation} from "react-router-dom";
import {
    getProjectDataPath,
    getProjectSettingsPath
} from "../../route/AppRouter";
import {Close as CloseIcon} from "@mui/icons-material";
import {
    getLastSettingsPageName,
    setLastSettingsPageName,
    setPathname
} from "../../utils/ConfigurationStorage";
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
import {
    getSettingsTabsBackgroundColor,
    getSettingsTabsColor
} from "../../utils/MaterialTableHelper";
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

const StyledTab = Tab;

export default function _ProjectSettingsTabs() {
    const location = useLocation();

    console.log('=== DEBUG ===');
    console.log('Current URL:', window.location.href);
    console.log('location.pathname:', location.pathname);
    console.log('getProjectSettingsPath():', getProjectSettingsPath());

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
                    textColor="secondary"
                    indicatorColor="secondary"
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
                </Tabs>
            </div>
            <div className={classes.panel}>
                <RolesProvider><GroupsProvider><BucketsProvider><UsersProvider><ClassesProvider>
                    <TagsProvider><ColumnsProvider> <EnumsProvider> <FiltersProvider> <TasksProvider>
                        <EventsProvider><ViewsProvider> <TeamsProvider> <SvgProvider>
                            <Routes>
                                <Route path="teams" element={<TeamsTab />} />
                                <Route path="users" element={<UsersTab />} />
                                <Route path="classes" element={<ClassesTab />} />
                                <Route path="icons" element={<SvgTab />} />
                                <Route path="enums" element={<EnumsTab />} />
                                <Route path="groups" element={<GroupsTab />} />
                                <Route path="buckets" element={<BucketsTab />} />
                                <Route path="tags" element={<TagsTab />} />
                                <Route path="columns" element={<ColumnsTab />} />
                                <Route path="filters" element={<FiltersTab />} />
                                <Route path="views" element={<ViewsTab />} />
                                <Route path="tasks" element={<TasksTab />} />
                                <Route path="*" element={<NotFoundPage />} />
                            </Routes>
                        </SvgProvider>
                        </TeamsProvider> </ViewsProvider> </EventsProvider>
                    </TasksProvider> </FiltersProvider> </EnumsProvider>
                    </ColumnsProvider> </TagsProvider> </ClassesProvider> </UsersProvider>
                </BucketsProvider> </GroupsProvider> </RolesProvider>
            </div>
        </Container>
    );
}
