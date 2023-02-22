import {Paper} from "@mui/material";
import Typography from "@mui/material/Typography";
import React from "react";
import makeStyles from '@mui/styles/makeStyles';

const useStyles = makeStyles(theme => ({
    PaperClass: {
        padding: '50px',
        alignItems: "center",
        display: "flex",
        flexDirection: "column"
    }
}));

export default function NotFoundPage() {
    const classes = useStyles();
    return (
        <div className="ContainerClass">
            <Paper className={classes.PaperClass} elevation={3}>
                <Typography variant="h1" component="h2" color='secondary'>404. That's an error</Typography>
                <Typography variant="h5">The requested URL was not found on this server.</Typography>
            </Paper>
        </div>
    );
}