import React, {useState} from 'react';
import { styled } from '@mui/material/styles';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import IconButton from '@mui/material/IconButton';
import DynamicIcon from '../utils/DynamicIcon';
import {Typography} from "@mui/material";

const PREFIX = 'FieldSelectEnum';

const classes = {
    paper: `${PREFIX}-paper`,
    root: `${PREFIX}-root`
};

const Root = styled('div')((
    {
        theme
    }
) => ({
    [`& .${classes.paper}`]: {
        border: '1px solid #d3d4d5',
    },

    [`& .${classes.root}`]: {
        // '&:focus': {
        //     backgroundColor: theme.palette.secondary.main,
        //     '& .MuiListItemIcon-root, & .MuiListItemText-primary': {
        //         color: theme.palette.common.white,
        //     },
        // },
    }
}));

const StyledMenu = (props => (
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

const StyledMenuItem = MenuItem;

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
        <Root>
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
                classes={{
                    paper: classes.paper
                }}>
                {items.map((item) => (
                    <StyledMenuItem
                        onClick={() => handleClose(item.value)}
                        selected={item.value === props.value}
                        key={item.value}
                        classes={{
                            root: classes.root
                        }}>
                        {iconsEnabled && <ListItemIcon> <DynamicIcon iconName={item.icon}/> </ListItemIcon>}
                        <ListItemText primary={item.text}/>
                    </StyledMenuItem>
                ))}
            </StyledMenu>
        </Root>
    );
}