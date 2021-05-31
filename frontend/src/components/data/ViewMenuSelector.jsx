import React, {useState} from 'react';
import PropTypes from "prop-types";
import MenuItem from "@material-ui/core/MenuItem";
import Typography from "@material-ui/core/Typography";
import {Grid, IconButton, Menu, Tooltip} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";

ViewMenuSelector.propTypes = {
    views: PropTypes.array.isRequired,
    activeView: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function ViewMenuSelector(props) {
    const classes = useStyles();

    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);

    const handleClick = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleSelected = (view) => {
        props.onChange(view);
    }

    const getViewName = (view) => {
        if (view.name.length > 17)
            return view.name.substring(0, 15) + "...";
        else
            return view.name;
    }

    const getSelectedDescription = () => {
        if (props.activeView.description != null && props.activeView.description.length > 0)
            return props.activeView.description;
        else
            return props.activeView.name;
    }

    return (
        <div className={classes.root}>
            <Grid container direction="row" alignItems="center" wrap={'nowrap'}>
                <Tooltip title={'Select view'}>
                    <IconButton
                        className={classes.select}
                        aria-controls="long-menu"
                        onClick={handleClick}
                        color={'inherit'}
                    >
                        <span className="material-icons">double_arrow</span>
                    </IconButton>
                </Tooltip>
                <Typography
                    variant="h6"
                    className={classes.description}
                >
                    {getSelectedDescription()}
                </Typography>
            </Grid>
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
                {props.views.map((view) => (
                    <MenuItem
                        key={view.id}
                        selected={view.id === props.activeView.id}
                        onClick={() => handleSelected(view)}
                    >
                        {getViewName(view)}
                    </MenuItem>
                ))}
            </Menu>
        </div>
    );
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