import React, {useContext, useState} from 'react';
import {
    AccountCircle,
    Brightness4 as SetDarkTheme,
    Brightness7 as SetLightTheme,
    FolderOpen,
    Logout,
    SwapHoriz
} from '@mui/icons-material';
import {getActiveProjectId, getRoles, getUsername} from "../../utils/ConfigurationStorage";
import {
    Box,
    Divider,
    IconButton,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Tooltip,
    Typography,
    useTheme
} from "@mui/material";
import {Link} from "react-router-dom";
import CustomThemeContext from "../../context/theme/CustomThemeContext";
import AccessContext from "../../context/access/AccessContext";
import {getGivenProjectDataPath} from "../../route/AppRouter";

// A user has one role; the token also holds the roles below it (e.g. "SUPER,ADMIN,MEMBER,ROBOT,AGENT"), show the highest
const ROLES_BY_LEVEL = ['SUPER', 'ADMIN', 'MEMBER', 'ROBOT', 'AGENT'];
const formatRole = (roles) => {
    const names = (roles ?? '').split(',').map(role => role.trim());
    const top = ROLES_BY_LEVEL.find(role => names.includes(role));
    return top ?? '';
};

const SectionTitle = ({children}) => (
    <Typography variant="overline" color="text.secondary" component="div" sx={{px: 2, pt: 1, lineHeight: 2}}>
        {children}
    </Typography>
);

// greys softer than the themes themselves, so the item doesn't stand out too much
const THEME_ITEM = {
    dark: {mx: 1, my: 0.5, borderRadius: 1, bgcolor: '#616161', color: '#ffffff', '&:hover': {bgcolor: '#757575'}},
    light: {mx: 1, my: 0.5, borderRadius: 1, bgcolor: '#bdbdbd', color: 'rgba(0, 0, 0, 0.87)', '&:hover': {bgcolor: '#cfcfcf'}}
};

export default function UserProfile(props) {
    let currentMode = useTheme().palette.mode;
    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);
    const {toggleColorMode} = useContext(CustomThemeContext);
    // also in Management, which has no project (and no AccessContext): then no project sections are shown
    const projects = useContext(AccessContext)?.projects;
    const activeProjectId = getActiveProjectId();
    const project = projects?.find(project => project.id === activeProjectId);
    const otherProjects = (projects ?? [])
        .filter(project => project.id !== activeProjectId)
        .sort((a, b) => a.name.localeCompare(b.name));
    const roles = formatRole(getRoles());

    const handleLogout = () => {
        setAnchorEl(null);
        props.onLogout();
    }

    const handleMenu = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleChangeProject = (projectId) => {
        setAnchorEl(null);
        props.onChangeProject(projectId);
    }

    return (
        <div>
            <Tooltip title={'User profile'}>
                <IconButton onClick={handleMenu} color={'inherit'} size="large">
                    <AccountCircle/>
                </IconButton>
            </Tooltip>
            <Menu
                id="user-menu"
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
                {/* who is signed in: not an action, so not a menu item */}
                <Box sx={{display: 'flex', alignItems: 'center', gap: 1.5, px: 2, pt: 0.5, pb: 1.5, minWidth: 200}}>
                    <AccountCircle color="action" fontSize="large"/>
                    <Box>
                        <Typography color="secondary" fontWeight={500}>{getUsername()}</Typography>
                        {roles !== '' && <Typography variant="caption" color="text.secondary">{roles}</Typography>}
                    </Box>
                </Box>
                {project != null && <Divider/>}
                {project != null && <SectionTitle>Current project</SectionTitle>}
                {/* where the user is: not clickable, so not a menu item */}
                {project != null &&
                    <Box sx={{display: 'flex', alignItems: 'center', px: 2, pt: 0.5, pb: 1}}>
                        <FolderOpen color="secondary" fontSize="small" sx={{mr: 1.5}}/>
                        <Typography color="secondary" fontWeight={500}>{project.name}</Typography>
                    </Box>
                }
                {otherProjects.length > 0 && <Divider/>}
                {otherProjects.length > 0 && <SectionTitle>Switch project</SectionTitle>}
                {/* many projects: the section scrolls instead of making the menu taller than the screen */}
                {otherProjects.length > 0 &&
                    <Box sx={{maxHeight: 320, overflowY: 'auto'}}>
                        {otherProjects.map(project => (
                            <MenuItem
                                key={project.id}
                                component={Link}
                                to={getGivenProjectDataPath(project.id)}
                                onClick={() => handleChangeProject(project.id)}
                            >
                                <ListItemIcon>
                                    <SwapHoriz fontSize="small"/>
                                </ListItemIcon>
                                <ListItemText>{project.name}</ListItemText>
                            </MenuItem>
                        ))}
                    </Box>
                }
                <Divider/>
                {/* in the colors of the theme it switches to (dark for Dark mode, light for Light mode), so it stands
                    out of the projects above */}
                <MenuItem onClick={toggleColorMode} sx={currentMode === 'light' ? THEME_ITEM.dark : THEME_ITEM.light}>
                    <ListItemIcon sx={{color: 'inherit'}}>
                        {currentMode === 'light' ? <SetDarkTheme fontSize="small"/> : <SetLightTheme fontSize="small"/>}
                    </ListItemIcon>
                    <ListItemText>{currentMode === 'light' ? "Dark mode" : "Light mode"}</ListItemText>
                </MenuItem>
                <MenuItem onClick={handleLogout}>
                    <ListItemIcon>
                        <Logout fontSize="small"/>
                    </ListItemIcon>
                    <ListItemText>Logout</ListItemText>
                </MenuItem>
            </Menu>
        </div>
    );
}
