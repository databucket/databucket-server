import React, {useState} from 'react';
import {makeStyles} from '@material-ui/core/styles';
import IconButton from '@material-ui/core/IconButton';
import Menu from '@material-ui/core/Menu';
import {ListItem, ListItemText, ListSubheader, Tooltip} from "@material-ui/core";
import List from "@material-ui/core/List";

const useStyles = makeStyles((theme) => ({
    root: {
        flexGrow: 1
    },
    availableTagsButton: {
        marginLeft: '10px',
        padding: theme.spacing(1)
    },
    content: {
        display: "flex",
        flexDirection: "column",
        justifyContent: "flex-start",
        alignItems: "flex-start",
        padding: "3px",
        margin: theme.spacing(1),
    }
}));


export default function AvailableTagsDialog(props) {

    const classes = useStyles();
    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);

    const handleMenu = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    return (
        <div className={classes.root}>
            <Tooltip title={'Available tags'}>
                <IconButton onClick={handleMenu} color={'inherit'} className={classes.availableTagsButton}>
                    <span className="material-icons">local_offer</span>
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
                    <List
                        sx={{width: '100%', maxWidth: 360, bgcolor: 'background.paper'}}
                        component="nav"
                        aria-labelledby="available-tags-list"
                        subheader={
                            <ListSubheader component="div" id="available-tags-list-subheader">
                                Available tags
                            </ListSubheader>
                        }
                    >
                        {props.bucketTags.map((tag) => (
                            <ListItem key={`tag-${tag.id}`}>
                                <ListItemText primary={`[${tag.id}] ${tag.name}`} />
                            </ListItem>
                            ))
                        }
                    </List>
                </div>
            </Menu>
        </div>
    );
}