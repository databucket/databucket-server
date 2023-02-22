import React, {useEffect, useState} from "react";
import Button from "@material-ui/core/Button";
import Typography from "@material-ui/core/Typography";
import Paper from "@material-ui/core/Paper";

export default function OauthLoginComponent({authOptions}) {
    return (
        <Paper className="PaperClass" elevation={3}>
            <Typography className="Title" variant="h5">
                Authentication Options
            </Typography>
            {authOptions.map(option => {
                return (<div key={option.name} className="ButtonLogin">
                    <Button
                        component="button"
                        color="inherit"
                        href="http://localhost:8080/oauth2/authorization/dopauth"
                        // href={option.url}
                    >
                        {option.name}
                    </Button>
                </div>)
            })}
        </Paper>
    );
}
