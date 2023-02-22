import React from "react";
import Grid from "@mui/material/Grid";
import {CircularProgress} from "@mui/material";

export const CenteredWaitingCircularProgress = () => {
    return (
        <Grid
            container
            spacing={0}
            direction="column"
            alignItems="center"
            justifyContent="center"
            style={{minHeight: '100vh'}}
        >
            <Grid item xs={3}>
                <CircularProgress disableShrink/>
            </Grid>
        </Grid>
    );
}