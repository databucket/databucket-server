import React, {useContext, useState} from 'react';
import {
    AccountCircle,
    Brightness4 as SetDarkTheme,
    Brightness7 as SetLightTheme
} from '@mui/icons-material';
import {getUsername} from "../../utils/ConfigurationStorage";
import {
    Button,
    IconButton,
    Menu,
    MenuItem,
    Tooltip,
    Typography,
    useTheme
} from "@mui/material";
import CustomThemeContext from "../../context/theme/CustomThemeContext";

export default function UserProfile(props) {
    let currentMode = useTheme().palette.mode;
    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);
    const {toggleColorMode} = useContext(CustomThemeContext);

    const handleLogout = () => {
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
                <MenuItem sx={{cursor: "default"}}>
                    <AccountCircle/>
                    <Typography color="secondary" style={{marginLeft: "10px"}}>{getUsername()}</Typography>
                </MenuItem>
                <MenuItem>
                    <Button
                        startIcon={currentMode === 'light' ? <SetDarkTheme/> : <SetLightTheme/>}
                        onClick={toggleColorMode}
                        color="inherit"
                    >
                        {currentMode === 'light' ? "Dark" : "Light"}
                    </Button>
                </MenuItem>
                <MenuItem>
                    <Button
                        variant="contained"
                        onClick={handleLogout}
                        color="primary"
                    >
                        Logout
                    </Button>
                </MenuItem>
            </Menu>
        </div>
    );
}
