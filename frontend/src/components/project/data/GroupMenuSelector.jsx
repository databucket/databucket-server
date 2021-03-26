import React, {useContext, useEffect} from 'react';
import PropTypes from "prop-types";
import ListItem from "@material-ui/core/ListItem";
import ListItemIcon from "@material-ui/core/ListItemIcon";
import ShowGroupsIcon from "@material-ui/icons/ArrowDropDownCircleOutlined";
import ListItemText from "@material-ui/core/ListItemText";
import List from "@material-ui/core/List";
import MenuItem from "@material-ui/core/MenuItem";
import Menu from "@material-ui/core/Menu";
import Divider from "@material-ui/core/Divider";
import AccessContext from "../../../context/access/AccessContext";

GroupMenuSelector.propTypes = {
    open: PropTypes.bool.isRequired // is the left list of buckets open?
}

export default function GroupMenuSelector(props) {

    const [anchorEl, setAnchorEl] = React.useState(null);
    const accessContext = useContext(AccessContext);
    const {accessTree, activeGroup, fetchAccessTree, setActiveGroup, views, fetchViews, columns, fetchColumns} = accessContext;

    useEffect(() => {
        if (accessTree == null)
            fetchAccessTree();
    }, [accessTree, fetchAccessTree]);

    useEffect(() => {
        if (accessTree != null && accessTree.views != null && views == null)
            fetchViews();
    }, [accessTree, views, fetchViews]);

    useEffect(() => {
        if (views != null && columns == null)
            fetchColumns();
    }, [views, columns, fetchColumns]);


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
        if (accessTree != null && accessTree.groups.length > 0) {
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

    if (accessTree != null && accessTree.groups.length > 0)
        return (
            <div>
                <List>
                    <ListItem button key={'key'} aria-controls="groups-menu" aria-haspopup="true" onClick={handleClick}>
                        {props.open === true ? (<ListItemIcon><ShowGroupsIcon/></ListItemIcon>) : (<div/>)}
                        <ListItemText primary={getSelectedName()}/>
                    </ListItem>
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
                    {accessTree.groups.map((group) => (
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