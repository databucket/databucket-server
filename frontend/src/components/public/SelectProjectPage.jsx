import React from "react";

import {hasSuperRole, setActiveProjectId} from "../../utils/ConfigurationStorage";
import {Link, useHistory} from "react-router-dom";
import {getManagementProjectsPath} from "../../route/AppRouter";
import {
    Avatar,
    Grid,
    IconButton,
    List,
    ListItem,
    ListItemAvatar,
    ListItemText,
    Paper,
    Tooltip,
    Typography
} from "@material-ui/core";
import {
    EventBusy as ExpiredProjectIcon,
    FolderSpecial as ActiveProjectIcon,
    NotInterested as DisabledProjectIcon
} from "@material-ui/icons";
import "./SelectProjectPage.css"

export default function SelectProjectsPage(props) {
    const projects = props.location.state.projects;
    let history = useHistory();

    const selectProject = (id) => {
        setActiveProjectId(id);
        history.push(`/project/${id}`)
    };
    return (
        <Paper className="ProjectsContainer" elevation={3}>
            <Grid className="TitleGrid"
                  container
                  direction="row"
                  justify="center"
                  alignItems="center"
                  spacing={1}>
                <Grid item>
                    <Typography variant="h5">
                        Select project
                    </Typography>
                </Grid>
                {hasSuperRole() ? (
                    <Grid item>
                        <Tooltip title="Manage accounts">
                            <IconButton component={Link} to={getManagementProjectsPath()}>
                                <span className="material-icons">manage_accounts</span>
                            </IconButton>
                        </Tooltip>
                    </Grid>
                ) : (
                    <div/>
                )}
            </Grid>

            <List component="nav"
                  className="ProjectsList">
                {projects.map(({id, name, description, enabled, expired}) => {
                    return (
                        <div key={id}>
                            <ListItem button onClick={() => selectProject(id)}>
                                <ListItemAvatar>
                                    <Avatar>
                                        {enabled !== true ? <DisabledProjectIcon/> : expired === true ?
                                            <ExpiredProjectIcon/> : <ActiveProjectIcon color='secondary'/>}
                                    </Avatar>
                                </ListItemAvatar>
                                <ListItemText primary={name} secondary={description}/>
                            </ListItem>
                        </div>
                    );
                })}
            </List>
        </Paper>
    );
}
