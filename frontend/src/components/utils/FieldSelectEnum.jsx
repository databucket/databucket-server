import React, {useState} from 'react';
import {withStyles} from '@material-ui/core/styles';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import IconButton from '@material-ui/core/IconButton';
import DynamicIcon from '../utils/DynamicIcon';
import {Typography} from "@material-ui/core";

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
                <IconButton
                    onClick={handleClick}
                    color="default"
                >
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