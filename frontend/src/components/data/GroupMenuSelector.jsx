import React, {useContext} from 'react';
import PropTypes from "prop-types";
import ListItemIcon from "@mui/material/ListItemIcon";
import ShowGroupsIcon from "@mui/icons-material/ArrowDropDownCircleOutlined";
import ListItemText from "@mui/material/ListItemText";
import List from "@mui/material/List";
import MenuItem from "@mui/material/MenuItem";
import Menu from "@mui/material/Menu";
import Divider from "@mui/material/Divider";
import AccessContext from "../../context/access/AccessContext";
import {ListItemButton} from "@mui/material";

GroupMenuSelector.propTypes = {
    open: PropTypes.bool.isRequired // is the left list of buckets open?
}

export default function GroupMenuSelector(props) {

    const [anchorEl, setAnchorEl] = React.useState(null);
    const accessContext = useContext(AccessContext);
    const {groups, activeGroup, setActiveGroup} = accessContext;

    const handleClick = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleSelected = (group) => {
        setActiveGroup(group);
        handleClose();
    }

    const getSelectedName = () => {
        if (groups != null && groups.length > 0) {
            if (props.open && activeGroup.name != null)
                return activeGroup.name.length > 17 ? activeGroup.name.substring(0, 15) + "..." : activeGroup.name;
            else
                return activeGroup.shortName;
        }
    }

    const getItemName = (name, shortName) => {
        if (name != null) {
            return `${shortName} - ${name}`;
        } else
            return shortName;
    }

    if (groups != null && groups.length > 0)
        return (
            <div>
                <List>
                    <ListItemButton key={'key'} aria-controls="groups-menu" aria-haspopup="true" onClick={handleClick}>
                        {props.open === true ? (<ListItemIcon><ShowGroupsIcon/></ListItemIcon>) : (<div/>)}
                        <ListItemText primary={getSelectedName()}/>
                    </ListItemButton>
                </List>
                <Divider/>
                <Menu
                    id="groups-menu"
                    anchorEl={anchorEl}
                    keepMounted
                    open={Boolean(anchorEl)}
                    onClose={handleClose}
                    PaperProps={{style: {minWidth: 100}}}
                >
                    {groups.sort((a, b) => {
                            return a.shortName > b.shortName ? 1 : -1
                        }).map((group) => (
                        <MenuItem
                            key={group.id}
                            selected={group.id === activeGroup.id}
                            onClick={() => handleSelected(group)}
                        >
                            {getItemName(group.name, group.shortName)}
                        </MenuItem>
                    ))}
                </Menu>
            </div>
        );
    else
        return (<div/>);
}
