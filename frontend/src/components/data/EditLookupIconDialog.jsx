import React, {useState} from 'react';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import IconButton from '@mui/material/IconButton';
import TableDynamicIcon from "../utils/TableDynamicIcon";


export default function LookupIconDialog(props) {
    const {onChange, items} = props;
    const [anchorEl, setAnchorEl] = useState(null);
    const [selectedIcon, setSelectedIcon] = useState(props.icon);

    const handleClick = event => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = (value, icon) => {
        setAnchorEl(null);
        if (value != null) {
            setSelectedIcon(icon);
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
                <TableDynamicIcon icon={selectedIcon != null ? selectedIcon : {name: 'more_horiz', color: null, svg: null}} />
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
                        selected={item.icon === selectedIcon}
                        key={item.value}
                    >
                        <ListItemIcon>
                            <TableDynamicIcon icon={item.icon}/>
                        </ListItemIcon>
                        <ListItemText primary={item.text}/>
                    </MenuItem>
                ))}
            </Menu>
        </div>
    );
}