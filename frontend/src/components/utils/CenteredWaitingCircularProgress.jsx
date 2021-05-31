import React from "react";
import Grid from "@material-ui/core/Grid";
import {CircularProgress} from "@material-ui/core";

export const CenteredWaitingCircularProgress = () => {
    return (
        <Grid
            container
            spacing={0}
            direction="column"
            alignItems="center"
            justify="center"
            style={{minHeight: '100vh'}}
        >
            <Grid item xs={3}>
                <CircularProgress disableShrink/>
            </Grid>
        </Grid>
    );
}