import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import IconButton from '@material-ui/core/IconButton';
import DynamicIcon from '../DynamicIcon';

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

export default function LookupIconDialog(props) {
    const { onChange, items } = props;
    const [anchorEl, setAnchorEl] = React.useState(null);
    const [selectedValue, setSelectedValue] = React.useState(props.selectedIconName);

    const handleClick = event => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = (value, iconName) => {
        setAnchorEl(null);
        if (value != null) {
            setSelectedValue(iconName);
            onChange(value);
        }        
    };

    return (
        <div>
            <IconButton
                onClick={handleClick}
                color="default"
            >
                <DynamicIcon iconName={selectedValue} />
            </IconButton>
            <StyledMenu
                id="customized-menu"
                anchorEl={anchorEl}
                keepMounted
                open={Boolean(anchorEl)}
                onClose={() => handleClose(null, null)}
            >
                {items.map((item) => (
                    <StyledMenuItem 
                        onClick={() => handleClose(item.key, item.icon_name)}
                        selected={item.icon_name === selectedValue}
                        key={item.key} >
                        <ListItemIcon>
                            <DynamicIcon iconName={item.icon_name} />
                        </ListItemIcon>
                        <ListItemText primary={item.key} />
                    </StyledMenuItem>
                ))}
            </StyledMenu>
        </div>
    );
}