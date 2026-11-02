import React, {useContext, useState} from 'react';
import {
    AccountCircle,
    Brightness4 as SetDarkTheme,
    Brightness7 as SetLightTheme,
    Logout
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
import CustomThemeContext from "../../context/theme/CustomThemeContext";
import AccessContext from "../../context/access/AccessContext";

// "MEMBER,ADMIN" -> "Member, Admin"
const formatRoles = (roles) => (roles ?? '').split(',')
    .map(role => role.trim())
    .filter(role => role !== '')
    .map(role => role.charAt(0) + role.slice(1).toLowerCase())
    .join(', ');

export default function UserProfile(props) {
    let currentMode = useTheme().palette.mode;
    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);
    const {toggleColorMode} = useContext(CustomThemeContext);
    // also in Management, which has no project (and no AccessContext): then no project is shown
    const projects = useContext(AccessContext)?.projects;
    const project = projects?.find(project => project.id === getActiveProjectId());
    const roles = formatRoles(getRoles());

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
                {/* who is signed in and where: not an action, so not a menu item */}
                <Box sx={{display: 'flex', alignItems: 'center', gap: 1.5, px: 2, pt: 0.5, pb: 1.5, minWidth: 200}}>
                    <AccountCircle color="action" fontSize="large"/>
                    <Box>
                        <Typography color="secondary" fontWeight={500}>{getUsername()}</Typography>
                        {project != null && <Typography variant="body2" color="text.secondary">{project.name}</Typography>}
                        {roles !== '' && <Typography variant="caption" color="text.secondary">{roles}</Typography>}
                    </Box>
                </Box>
                <Divider/>
                <MenuItem onClick={toggleColorMode}>
                    <ListItemIcon>
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
