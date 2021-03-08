import React, {useContext, useState} from 'react';
import IconButton from '@material-ui/core/IconButton';
import ProjectIcon from '@material-ui/icons/LocalParking';
import Menu from '@material-ui/core/Menu';
import MenuItem from "@material-ui/core/MenuItem";
import AccessTreeContext from "../../../context/accessTree/AccessTreeContext";
import {getActiveProjectId} from "../../../utils/ConfigurationStorage";

export default function UserProjects(props) {

    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);
    const accessTreeContext = useContext(AccessTreeContext);
    const {accessTree} = accessTreeContext;

    document.title = accessTree != null && accessTree.projects != null ? 'Databucket - ' + accessTree.projects.find(project => project.id === getActiveProjectId()).name : 'Databucket';

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

    if (accessTree != null && accessTree.projects != null && accessTree.projects.length > 1)
        return (
            <div>
                <IconButton
                    aria-label="account of current user"
                    aria-controls="menu-appbar"
                    aria-haspopup="true"
                    onClick={handleMenu}
                    color="inherit"
                >
                    <ProjectIcon/>
                </IconButton>
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
                    {accessTree.projects.sort((a, b) => {
                        return a.name > b.name ? 1 : -1
                    }).map((project) => (
                        <MenuItem
                            key={project.id}
                            value={project.id}
                            selected={project.id === getActiveProjectId()}
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