import {Paper} from "@mui/material";
import {styled} from '@mui/material/styles';
import Typography from "@mui/material/Typography";
import React from "react";

const PREFIX = 'NotFoundPage';

const classes = {
    PaperClass: `${PREFIX}-PaperClass`
};

const Root = styled('div')(() => ({
    [`& .${classes.PaperClass}`]: {
        padding: '50px',
        alignItems: "center",
        display: "flex",
        flexDirection: "column"
    }
}));

export default function NotFoundPage() {

    return (
        <Root className="ContainerClass">
            <Paper className={classes.PaperClass} elevation={3}>
                <Typography variant="h1" component="h2" color='secondary'>404. That's an error</Typography>
                <Typography variant="h5">The requested URL was not found on this server.</Typography>
            </Paper>
        </Root>
    );
}
