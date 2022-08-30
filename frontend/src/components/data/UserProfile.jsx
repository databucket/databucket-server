import React, {useContext, useState} from 'react';
import {makeStyles} from '@material-ui/core/styles';
import IconButton from '@material-ui/core/IconButton';
import AccountCircle from '@material-ui/icons/AccountCircle';
import Menu from '@material-ui/core/Menu';
import Typography from "@material-ui/core/Typography";
import SetLightTheme from "@material-ui/icons/Brightness7";
import SetDarkTheme from "@material-ui/icons/Brightness4";
import {getUsername, saveThemeName} from "../../utils/ConfigurationStorage";
import CustomThemeContext from "../../context/theme/CustomThemeContext";
import Button from "@material-ui/core/Button";
import {Tooltip} from "@material-ui/core";

const useStyles = makeStyles((theme) => ({
    oneLine: {
        display: 'flex',
        alignItems: 'center',
        flexWrap: 'wrap'
    },
    content: {
        display: "flex",
        flexDirection: "column",
        justifyContent: "flex-start",
        alignItems: "flex-start",
        padding: "15px",
        margin: theme.spacing(1),
    },
    button: {
        marginTop: "13px"
    }
}));

export default function UserProfile(props) {
    const classes = useStyles();
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
        <div>
            <Tooltip title={'User profile'}>
                <IconButton onClick={handleMenu} color={'inherit'}>
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
        </div>
    );
}