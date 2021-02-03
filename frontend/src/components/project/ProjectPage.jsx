import React from 'react';
import {Paper} from "@material-ui/core";
import Typography from "@material-ui/core/Typography";
import {isLogin, logOut} from '../../utils/ConfigurationStorage';
import Button from "@material-ui/core/Button";
import {Redirect} from "react-router-dom";

export default function ProjectPage() {

    if (isLogin()) {
        return (
            <div className="login-page-paper">
                <Paper className="Login" elevation={3}>
                    <Typography variant="h2">
                        Main Databucket Page
                    </Typography>
                    <Button onClick={logOut()}>Logout</Button>
                </Paper>
            </div>
        );
    } else {
        return (<Redirect to="/login"/>);
    }

}
