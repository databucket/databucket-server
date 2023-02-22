import React from 'react';
import {Typography} from "@mui/material";
import makeStyles from '@mui/styles/makeStyles';

export default function MissingActiveView() {
    const classes = useStyles();
    return (
        <Typography
            variant="h6"
            color={'error'}
            className={classes.message}
        >
            {'You do not have permission to any view of this bucket.'}
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