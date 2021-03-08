import React from 'react';
import PropTypes from "prop-types";
import MenuItem from "@material-ui/core/MenuItem";
import Typography from "@material-ui/core/Typography";
import {Grid, Select} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";

ViewMenuSelector.propTypes = {
    views: PropTypes.array.isRequired,
    selectedId: PropTypes.number.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function ViewMenuSelector(props) {
    const classes = useStyles();

    const handleSelected = (view) => {
        props.onChange(view.id);
    }

    const getViewName = (view) => {
        if (view.name.length > 17)
            return view.name.substring(0, 15) + "...";
        else
            return view.name;
    }

    const getSelectedDescription = () => {
        if (props.views.length > 0 && props.selectedId > 0) {
            const selectedView = props.views.find(view => view.id === props.selectedId);
            if (selectedView != null)
                if (selectedView.description.length > 40)
                    return selectedView.description.substring(0, 40) + "...";
                else
                    return selectedView.description;
        } else
            return '';
    }

    if (props.views.length > 0)
        return (
            <div className={classes.root}>
                <Grid container direction="row" alignItems="center">
                    {/*<Typography className={classes.view}>View:</Typography>*/}
                    <Select className={classes.select} value={props.selectedId}>
                        {props.views.map((view) => (
                            <MenuItem
                                key={view.id}
                                value={view.id}
                                onClick={() => handleSelected(view)}
                            >
                                {getViewName(view)}
                            </MenuItem>
                        ))}
                    </Select>
                    <Typography
                        variant="h6"
                        className={classes.description}
                    >
                        {getSelectedDescription()}
                    </Typography>
                </Grid>
            </div>
        );
    else
        return (<Typography>- no views -</Typography>);
};

const useStyles = makeStyles((theme) => ({
    root: {
        flexGrow: 1,
    },
    view: {
        paddingLeft: theme.spacing(2),
        padding: theme.spacing(1)
    },
    select: {
        marginLeft: '20px',
        marginRight: '10px',
        padding: theme.spacing(0)
    },
    description: {
        padding: theme.spacing(2),
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis",
    }
}));