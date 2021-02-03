import React, {Component} from 'react';
import {Paper} from "@material-ui/core";
import Typography from "@material-ui/core/Typography";

export default class DataDetailsPage extends Component {
    render() {
        return (
            <div className="login-page-paper">
                <Paper className="Login" elevation={3}>
                    <Typography variant="h2">
                        MainPage
                    </Typography>
                </Paper>
            </div>
        );
    }
}
