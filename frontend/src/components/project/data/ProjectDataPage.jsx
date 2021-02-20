import React, {useState} from 'react';
import {Paper} from "@material-ui/core";
import Typography from "@material-ui/core/Typography";
import {
    getLastManagementPageName,
    getLastSettingsPageName,
    isLogin,
    logOut
} from '../../../utils/ConfigurationStorage';
import Button from "@material-ui/core/Button";
import {Link, Redirect, useParams} from "react-router-dom";
import {getProjectSettingsPath} from "../../../route/AppRouter";
import UserProfile from "./UserProfile";

export default function ProjectDataPage() {

    const [logged, setLogged] = useState(isLogin());
    let {id} = useParams();

    const handleLogout = () => {
        logOut();
        setLogged(isLogin());
    }

    if (logged) {
        return (
            <div className="login-page-paper">
                <Paper className="Login" elevation={3}>
                    <Typography variant="h2">
                        {'Data for project ' + id}
                    </Typography>
                    <Button component={Link}
                            to={getProjectSettingsPath() + "/" + getLastSettingsPageName()}>Settings</Button>
                    <Button component={Link} to={"/management/" + getLastManagementPageName()}>Management</Button>
                    <UserProfile onLogout={handleLogout}/>
                </Paper>
            </div>
        );
    } else {
        return (<Redirect to="/login"/>);
    }

}
