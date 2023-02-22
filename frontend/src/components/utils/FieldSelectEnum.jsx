import React, {useState} from 'react';
import withStyles from '@mui/styles/withStyles';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import IconButton from '@mui/material/IconButton';
import DynamicIcon from '../utils/DynamicIcon';
import {Typography} from "@mui/material";

const StyledMenu = withStyles({
    paper: {
        border: '1px solid #d3d4d5',
    },
})(props => (
    <Menu
        // elevation={10}
        getContentAnchorEl={null}
        anchorOrigin={{
            vertical: 'top',
            horizontal: 'center',
        }}
        transformOrigin={{
            vertical: 'top',
            horizontal: 'center',
        }}
        {...props}
    />
));

const StyledMenuItem = withStyles(theme => ({
    root: {
        // '&:focus': {
        //     backgroundColor: theme.palette.secondary.main,
        //     '& .MuiListItemIcon-root, & .MuiListItemText-primary': {
        //         color: theme.palette.common.white,
        //     },
        // },
    },
}))(MenuItem);

export default function FieldSelectEnum(props) {

    const [anchorEl, setAnchorEl] = useState(null);
    const items = props.enumDef.items;
    const iconsEnabled = props.enumDef.iconsEnabled;

    const handleClick = event => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = (value) => {
        setAnchorEl(null);
        props.onChange(value);
    };

    return (
        <div>
            {iconsEnabled ?
                <IconButton onClick={handleClick} color="default" size="large">
                    <DynamicIcon iconName={items.filter(item => item.value === props.value)[0].icon}/>
                </IconButton>
                :
                <Typography>{props.value}</Typography>
            }
            <StyledMenu
                id="enum-menu"
                anchorEl={anchorEl}
                keepMounted
                open={Boolean(anchorEl)}
                onClose={() => handleClose(null, null)}
            >
                {items.map((item) => (
                    <StyledMenuItem
                        onClick={() => handleClose(item.value)}
                        selected={item.value === props.value}
                        key={item.value}
                    >
                        {iconsEnabled && <ListItemIcon> <DynamicIcon iconName={item.icon}/> </ListItemIcon>}
                        <ListItemText primary={item.text}/>
                    </StyledMenuItem>
                ))}
            </StyledMenu>
        </div>
    );
}