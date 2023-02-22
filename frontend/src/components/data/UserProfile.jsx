import React, {useContext, useState} from 'react';
import { styled } from '@mui/material/styles';
import IconButton from '@mui/material/IconButton';
import AccountCircle from '@mui/icons-material/AccountCircle';
import Menu from '@mui/material/Menu';
import Typography from "@mui/material/Typography";
import SetLightTheme from "@mui/icons-material/Brightness7";
import SetDarkTheme from "@mui/icons-material/Brightness4";
import {getUsername, saveThemeName} from "../../utils/ConfigurationStorage";
import CustomThemeContext from "../../context/theme/CustomThemeContext";
import Button from "@mui/material/Button";
import {Tooltip} from "@mui/material";

const PREFIX = 'UserProfile';

const classes = {
    oneLine: `${PREFIX}-oneLine`,
    content: `${PREFIX}-content`,
    button: `${PREFIX}-button`
};

const Root = styled('div')((
    {
        theme
    }
) => ({
    [`& .${classes.oneLine}`]: {
        display: 'flex',
        alignItems: 'center',
        flexWrap: 'wrap'
    },

    [`& .${classes.content}`]: {
        display: "flex",
        flexDirection: "column",
        justifyContent: "flex-start",
        alignItems: "flex-start",
        padding: "15px",
        margin: theme.spacing(1),
    },

    [`& .${classes.button}`]: {
        marginTop: "13px"
    }
}));

export default function UserProfile(props) {

    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);
    const [customThemeName] = useContext(CustomThemeContext);

    const handleLogout = () => {
        props.onLogout();
    }

    const handleChangeTheme = () => {
        let newThemeName;
        if (customThemeName === 'light')
            newThemeName = 'dark';
        else
            newThemeName = 'light';

        saveThemeName(newThemeName);
        window.location.reload();
    };

    const handleMenu = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    return (
        <Root>
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
                <div className={classes.content}>
                    <div className={classes.oneLine}>
                        <AccountCircle/>
                        <Typography color="secondary" style={{marginLeft: "10px"}}>{getUsername()}</Typography>
                    </div>
                    <Button
                        className={classes.button}
                        startIcon={customThemeName === 'light' ? <SetDarkTheme/> : <SetLightTheme/>}
                        onClick={handleChangeTheme}
                    >
                        {customThemeName === 'light' ? "Dark" : "Light"}
                    </Button>
                    <Button
                        variant="contained"
                        className={classes.button}
                        onClick={handleLogout}
                        color="primary"
                    >
                        Logout
                    </Button>
                </div>
            </Menu>
        </Root>
    );
}