import React, {useState} from 'react';
import {styled, Tab, Tabs} from '@mui/material';

import {
    getSettingsTabsBackgroundColor,
    getSettingsTabsColor
} from "../../../utils/MaterialTableHelper";
import TemplConfigTeamsTab from "./TemplConfigTeamsTab";
import PropTypes from "prop-types";
import TemplConfigClassesTab from "./TemplConfigClassesTab";
import TemplConfigEnumsTab from "./TemplConfigEnumsTab";
import TemplConfigGroupsTab from "./TemplConfigGroupsTab";
import TemplConfigBucketsTab from "./TemplConfigBucketsTab";
import TemplConfigTagsTab from "./TemplConfigTagsTab";
import TemplConfigColumnsTab from "./TemplConfigColumnsTab";
import TemplConfigFiltersTab from "./TemplConfigFiltersTab";
import TemplConfigViewsTab from "./TemplConfigViewsTab";
import TemplConfigTasksTab from "./TemplConfigTasksTab";
import TemplConfigDataTab from "./TemplConfigDataTab";

const PREFIX = '_TemplConfigTabs';

const classes = {
    root: `${PREFIX}-root`,
    selected: `${PREFIX}-selected`,
    root2: `${PREFIX}-root2`,
    tabs: `${PREFIX}-tabs`,
    panel: `${PREFIX}-panel`
};

const Root = styled('div')((
    {
        theme
    }
) => ({
    [`& .${classes.root2}`]: {
        flexGrow: 1,
        display: 'flex'
    },

    [`& .${classes.tabs}`]: {
        color: getSettingsTabsColor(theme),
        backgroundColor: getSettingsTabsBackgroundColor(theme),
        borderRight: `1px solid ${theme.palette.divider}`,
        width: '10%'
    },

    [`& .${classes.panel}`]: {
        width: '90%'
    }
}));

const StyledTab = Tab

_TemplConfigTabs.propTypes = {
    template: PropTypes.object.isRequired,
    setTemplate: PropTypes.func.isRequired
}

export default function _TemplConfigTabs(props) {


    const [activeTab, setActiveTab] = useState('1');

    const handleChangeTab = (event, newActiveTab) => {
        setActiveTab(newActiveTab);
    }

    return (
        <Root>
            <div className={classes.root}>
                <div className={classes.tabs}>
                    <Tabs
                        value={activeTab}
                        onChange={handleChangeTab}
                        variant="scrollable"
                        scrollButtons
                        orientation={'vertical'}
                        allowScrollButtonsMobile>
                        <StyledTab value="1" label="Teams"/>
                        <StyledTab value="2" label="Enums"/>
                        <StyledTab value="3" label="Classes"/>
                        <StyledTab value="4" label="Groups"/>
                        <StyledTab value="5" label="Buckets"/>
                        <StyledTab value="6" label="Tags"/>
                        <StyledTab value="7" label="Columns"/>
                        <StyledTab value="8" label="Filters"/>
                        <StyledTab value="9" label="Views"/>
                        <StyledTab value="10" label="Tasks"/>
                        <StyledTab value="11" label="Data"/>
                    </Tabs>
                </div>
                <div className={classes.panel}>
                    {activeTab === '1' &&
                        <TemplConfigTeamsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '2' &&
                        <TemplConfigEnumsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '3' &&
                        <TemplConfigClassesTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '4' &&
                        <TemplConfigGroupsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '5' &&
                        <TemplConfigBucketsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '6' &&
                        <TemplConfigTagsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '7' &&
                        <TemplConfigColumnsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '8' &&
                        <TemplConfigFiltersTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '9' &&
                        <TemplConfigViewsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '10' &&
                        <TemplConfigTasksTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '11' &&
                        <TemplConfigDataTab template={props.template} setTemplate={props.setTemplate}/>}
                </div>
            </div>
        </Root>
    );
}
