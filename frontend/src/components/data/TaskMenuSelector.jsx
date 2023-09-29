import React, {useState} from 'react';
import PropTypes from "prop-types";
import {IconButton, Menu, MenuItem, styled, Tooltip} from "@mui/material";

const PREFIX = 'TaskMenuSelector';

const classes = {
    view: `${PREFIX}-view`,
    select: `${PREFIX}-select`,
    description: `${PREFIX}-description`
};

const Root = styled('div')(({theme}) => ({
    flexGrow: 1,

    [`& .${classes.view}`]: {
        paddingLeft: theme.spacing(2),
        padding: theme.spacing(1)
    },

    [`& .${classes.select}`]: {
        marginLeft: '10px',
        padding: theme.spacing(1)
    },

    [`& .${classes.description}`]: {
        padding: theme.spacing(2),
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis"
    }
}));

TaskMenuSelector.propTypes = {
    tasks: PropTypes.array.isRequired,
    onTaskSelected: PropTypes.func.isRequired
}

export default function TaskMenuSelector(props) {


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
            <Root>
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
            </Root>
        );
    else
        return (<div/>);
}
