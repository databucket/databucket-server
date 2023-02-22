import React, {useState} from 'react';
import PropTypes from "prop-types";
import MenuItem from "@mui/material/MenuItem";
import {IconButton, Menu, Tooltip} from "@mui/material";
import makeStyles from '@mui/styles/makeStyles';

TaskMenuSelector.propTypes = {
    tasks: PropTypes.array.isRequired,
    onTaskSelected: PropTypes.func.isRequired
}

export default function TaskMenuSelector(props) {
    const classes = useStyles();

    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);

    const handleClick = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleSelected = (task) => {
        props.onTaskSelected(task);
        handleClose();
    }

    if (props.tasks != null && props.tasks.length > 0)
        return (
            <div className={classes.root}>
                <Tooltip title={'Select task'}>
                    <IconButton
                        className={classes.select}
                        aria-controls="long-menu"
                        onClick={handleClick}
                        color={'inherit'}
                        size="large">
                        <span className="material-icons">arrow_circle_down</span>
                    </IconButton>
                </Tooltip>
                <Menu
                    id="long-menu"
                    anchorEl={anchorEl}
                    keepMounted
                    open={open}
                    onClose={handleClose}
                    PaperProps={{
                        style: {
                            // maxHeight: 48 * 4.5,
                            minWidth: '15ch'
                        },
                    }}
                >
                    {props.tasks.map((task) => (
                        <MenuItem
                            key={task.id}
                            selected={false}
                            onClick={() => handleSelected(task)}
                        >
                            {task.name}
                        </MenuItem>
                    ))}
                </Menu>
            </div>
        );
    else
        return (<div/>);
};

const useStyles = makeStyles((theme) => ({
    root: {
        flexGrow: 1
    },
    view: {
        paddingLeft: theme.spacing(2),
        padding: theme.spacing(1)
    },
    select: {
        marginLeft: '10px',
        padding: theme.spacing(1)
    },
    description: {
        padding: theme.spacing(2),
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis"
    }
}));