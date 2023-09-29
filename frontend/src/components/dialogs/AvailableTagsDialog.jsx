import React, {useState} from 'react';
import {
    IconButton,
    List,
    ListItem,
    ListItemText,
    ListSubheader,
    Menu,
    styled,
    Tooltip
} from '@mui/material';

const PREFIX = 'AvailableTagsDialog';

const classes = {
    availableTagsButton: `${PREFIX}-availableTagsButton`,
    content: `${PREFIX}-content`
};

const Root = styled('div')(({theme}) => ({
    flexGrow: 1,

    [`& .${classes.availableTagsButton}`]: {
        marginLeft: '10px',
        padding: theme.spacing(1)
    },

    [`& .${classes.content}`]: {
        display: "flex",
        flexDirection: "column",
        justifyContent: "flex-start",
        alignItems: "flex-start",
        padding: "3px",
        margin: theme.spacing(1),
    }
}));


export default function AvailableTagsDialog(props) {


    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);

    const handleMenu = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    return (
        <Root>
            <Tooltip title={'Available tags'}>
                <IconButton
                    onClick={handleMenu}
                    color={'inherit'}
                    className={classes.availableTagsButton}
                    size="large">
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
                                <ListItemText primary={`[${tag.id}] ${tag.name}`}/>
                            </ListItem>
                        ))
                        }
                    </List>
                </div>
            </Menu>
        </Root>
    );
}
