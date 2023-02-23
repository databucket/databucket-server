import React, {useContext, useState} from 'react';
import IconButton from '@mui/material/IconButton';
import AccountCircle from '@mui/icons-material/AccountCircle';
import Menu from '@mui/material/Menu';
import Typography from "@mui/material/Typography";
import SetLightTheme from "@mui/icons-material/Brightness7";
import SetDarkTheme from "@mui/icons-material/Brightness4";
import {getUsername} from "../../utils/ConfigurationStorage";
import Button from "@mui/material/Button";
import {Tooltip} from "@mui/material";
import MenuItem from "@mui/material/MenuItem";
import CustomThemeContext from "../../context/theme/CustomThemeContext";
import {useTheme} from "@mui/material/styles";


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
