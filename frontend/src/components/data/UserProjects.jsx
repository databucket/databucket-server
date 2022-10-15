import React, {useContext, useState} from 'react';
import IconButton from '@material-ui/core/IconButton';
import Menu from '@material-ui/core/Menu';
import MenuItem from "@material-ui/core/MenuItem";
import AccessContext from "../../context/access/AccessContext";
import {getActiveProjectId} from "../../utils/ConfigurationStorage";
import {Tooltip} from "@material-ui/core";
import {Link} from "react-router-dom";
import {getGivenProjectDataPath} from "../../route/AppRouter";

export default function UserProjects(props) {

    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);
    const accessContext = useContext(AccessContext);
    const {projects} = accessContext;

    document.title = (projects != null && projects.length > 0) ? 'Databucket - ' + projects.find(project => project.id === getActiveProjectId()).name : 'Databucket';

    const handleMenu = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleSelected = (project) => {
        if (project.id !== getActiveProjectId()) {
            props.onChangeProject(project.id);
        }
    }

    if (projects != null && projects.length > 1)
        return (
            <div>
                <Tooltip title={'Open project'}>
                    <IconButton onClick={handleMenu} color={'inherit'}>
                        <span className="material-icons">exit_to_app</span>
                    </IconButton>
                </Tooltip>
                <Menu
                    id="menu-appbar"
                    anchorEl={anchorEl}
                    anchorOrigin={{
                        vertical: 'top',
                        horizontal: 'right',
                    }}
                    keepMounted
                    transformOrigin={{
                        vertical: 'top',
                        horizontal: 'right',
                    }}
                    open={open}
                    onClose={handleClose}
                >
                    {projects
                        .filter(project => project.id !== getActiveProjectId())
                        .sort((a, b) => {
                            return a.name > b.name ? 1 : -1
                        })
                        .map((project) => (
                            <MenuItem
                                component={Link}
                                key={project.id}
                                value={project.id}
                                to={getGivenProjectDataPath(project.id)}
                                onClick={() => handleSelected(project)}
                            >
                                {project.name}
                            </MenuItem>
                        ))}
                </Menu>
            </div>
        );
    else
        return <div/>;
}