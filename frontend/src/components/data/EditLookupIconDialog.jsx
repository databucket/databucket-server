import React, {useState} from 'react';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import IconButton from '@material-ui/core/IconButton';
import TableDynamicIcon from "../utils/TableDynamicIcon";


export default function LookupIconDialog(props) {
    const {onChange, items} = props;
    const [anchorEl, setAnchorEl] = useState(null);
    const [selectedValue, setSelectedValue] = useState(props.selectedIconName);

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
                size="small"
            >
                <TableDynamicIcon iconName={selectedValue != null ? selectedValue : 'more_horiz'}/>
            </IconButton>
            <Menu
                id="customized-menu"
                anchorEl={anchorEl}
                keepMounted
                open={Boolean(anchorEl)}
                onClose={() => handleClose(null, null)}
            >
                {items.map((item) => (
                    <MenuItem
                        onClick={() => handleClose(item.value, item.icon)}
                        selected={item.icon === selectedValue}
                        key={item.value}
                    >
                        <ListItemIcon>
                            <TableDynamicIcon iconName={item.icon}/>
                        </ListItemIcon>
                        <ListItemText primary={item.text}/>
                    </MenuItem>
                ))}
            </Menu>
        </div>
    );
}