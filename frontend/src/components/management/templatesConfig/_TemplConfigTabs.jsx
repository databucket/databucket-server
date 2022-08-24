import React, {useState} from 'react';
import Tab from "@material-ui/core/Tab";
import Tabs from "@material-ui/core/Tabs";
import {makeStyles, withStyles} from "@material-ui/core/styles";

import {
    getSettingsTabHooverBackgroundColor,
    getSettingsTabsBackgroundColor,
    getSettingsTabsColor,
    getSettingsTabSelectedBackgroundColor,
    getSettingsTabSelectedColor
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

const useStyles = makeStyles((theme) => ({
    root: {
        flexGrow: 1,
        display: 'flex'
    },
    tabs: {
        color: getSettingsTabsColor(theme),
        backgroundColor: getSettingsTabsBackgroundColor(theme),
        borderRight: `1px solid ${theme.palette.divider}`,
        width: '10%'
    },
    panel: {
        width: '90%'
    }

}));

const styles = theme => ({
    root: {
        "&:hover": {
            backgroundColor: getSettingsTabHooverBackgroundColor(theme),
            opacity: 1
        },
        "&$selected": {
            backgroundColor: getSettingsTabSelectedBackgroundColor(theme),
            color: getSettingsTabSelectedColor(theme),
        },
        textTransform: "initial"
    },
    selected: {}
});

const StyledTab = withStyles(styles)(Tab)

_TemplConfigTabs.propTypes = {
    template: PropTypes.object.isRequired,
    setTemplate: PropTypes.func.isRequired
}

export default function _TemplConfigTabs(props) {

    const classes = useStyles();
    const [activeTab, setActiveTab] = useState('1');

    const handleChangeTab = (event, newActiveTab) => {
        setActiveTab(newActiveTab);
    }

    return (
        <div>
            <div className={classes.root}>
                <div className={classes.tabs}>
                    <Tabs
                        value={activeTab}
                        onChange={handleChangeTab}
                        variant="scrollable"
                        scrollButtons="on"
                        orientation={'vertical'}
                    >
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
                    {activeTab === '1' && <TemplConfigTeamsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '2' && <TemplConfigEnumsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '3' && <TemplConfigClassesTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '4' && <TemplConfigGroupsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '5' && <TemplConfigBucketsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '6' && <TemplConfigTagsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '7' && <TemplConfigColumnsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '8' && <TemplConfigFiltersTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '9' && <TemplConfigViewsTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '10' && <TemplConfigTasksTab template={props.template} setTemplate={props.setTemplate}/>}
                    {activeTab === '11' && <TemplConfigDataTab template={props.template} setTemplate={props.setTemplate}/>}
                </div>
            </div>
        </div>
    );
}