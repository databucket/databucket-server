import React from 'react';
import {Typography} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";

export default function MissingBucketTable() {
    const classes = useStyles();
    return (
        <Typography
            variant="h6"
            color={'error'}
            className={classes.message}
        >
            {'You do not have permission to any bucket.'}
        </Typography>);
}

const useStyles = makeStyles((theme) => ({
    message: {
        padding: theme.spacing(3),
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis",
    }
}));