import React, {useState} from 'react';
import PropTypes from "prop-types";
import MenuItem from "@material-ui/core/MenuItem";
import Typography from "@material-ui/core/Typography";
import {Grid, IconButton, Menu} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";
import ViewListIcon from "@material-ui/icons/MoreVert";

ViewMenuSelector.propTypes = {
    views: PropTypes.array.isRequired,
    activeView: PropTypes.object,
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
        if (props.activeView != null)
            if (props.activeView.description != null && props.activeView.description.length > 0)
                return props.activeView.description;
            else
                return props.activeView.name;
        else
            return '---'
    }

    if (props.views.length > 0)
        return (
            <div className={classes.root}>
                <Grid container direction="row" alignItems="center" wrap={'nowrap'}>
                    <IconButton
                        className={classes.select}
                        aria-label="more"
                        aria-controls="long-menu"
                        aria-haspopup="true"
                        onClick={handleClick}
                    >
                        <ViewListIcon/>
                    </IconButton>
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
    else
        return (
            <Typography
                variant="h6"
                color={'error'}
                className={classes.description}
            >
                {'You do not have permission to any view of this bucket.'}
            </Typography>);
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
        marginLeft: '5px',
        padding: theme.spacing(1)
    },
    description: {
        padding: theme.spacing(2),
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis",
    }
}));