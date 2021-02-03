import React, {useEffect} from 'react';
import {makeStyles} from "@material-ui/core/styles";
import AppBar from "@material-ui/core/AppBar";
import Toolbar from "@material-ui/core/Toolbar";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import ProjectsTabHook from "./ProjectsTab";
import UsersTabHook from "./UsersTab";
import IconButton from "@material-ui/core/IconButton";
import CloseIcon from "@material-ui/icons/Close";
import {Link} from "react-router-dom";
import {getBaseUrl, getGetOptions} from "../../utils/MaterialTableHelper";
import {getAppBarBackgroundColor} from "../../utils/Themes";

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

function ManagementTabs() {
    const classes = useStyles();
    const [value, setValue] = React.useState(0);
    const [roles, setRoles] = React.useState(null);

    useEffect(() => {
        fetch(getBaseUrl('users/roles'), getGetOptions())
            .then(response => response.json())
            .then(roles => setRoles(roles));
    }, []);

    const handleChange = (event, newValue) => {
        setValue(newValue);
    }

    return (
        <div>
            <AppBar className={classes.appBar}>
                <Toolbar variant="dense">
                    <IconButton color="inherit" edge="start" component={Link} to={'/project/'} aria-label="Close">
                        <CloseIcon/>
                    </IconButton>
                    <Tabs
                        value={value}
                        onChange={handleChange}
                        variant="scrollable"
                        scrollButtons="on"
                        className={classes.tabs}
                    >
                        <Tab label="Projects"/>
                        <Tab label="Users"/>
                    </Tabs>
                </Toolbar>
            </AppBar>
            {value === 0 && <ProjectsTabHook />}
            {value === 1 && <UsersTabHook roles={roles}/>}
        </div>
    );
}

export default ManagementTabs;
