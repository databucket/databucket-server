import React, {useState} from 'react';
import {styled} from '@mui/material/styles';
import PropTypes from "prop-types";
import MenuItem from "@mui/material/MenuItem";
import Typography from "@mui/material/Typography";
import {Grid, IconButton, Menu, Tooltip} from "@mui/material";

const PREFIX = 'ViewMenuSelector';

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

ViewMenuSelector.propTypes = {
    views: PropTypes.array.isRequired,
    activeView: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function ViewMenuSelector(props) {


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
        <Root>
            <Grid container direction="row" alignItems="center" wrap={'nowrap'}>
                {props.views.length > 1 &&
                    <Tooltip title={'Select view'}>
                        <IconButton
                            className={classes.select}
                            aria-controls="long-menu"
                            onClick={handleClick}
                            color={'inherit'}
                            size="large">
                            <span className="material-icons">double_arrow</span>
                        </IconButton>
                    </Tooltip>
                }
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
        </Root>
    );
}
