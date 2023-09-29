import React, {useEffect, useState} from "react";
import Logo from "../../images/databucket-logo.png";
import {MessageBox} from "../utils/MessageBox";
import {getBaseUrl} from "../../utils/UrlBuilder";
import OauthLoginComponent from "../auth/OauthLogin";
import {Box, Divider, Paper, Typography} from "@mui/material";
import LoginFormComponent from "../auth/LoginForm";
import "./AuthPage.css"
import {useParams} from "react-router-dom";

export default function AuthPage() {

    const {error} = useParams();
    useEffect(() => {
        if (!!error) {
            setMessageBox({open: true, severity: 'error', title: 'Login failed', message: ""});
        }
    }, [error])
    const [authOptions, setAuthOptions] = useState([]);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    useEffect(() => {
        fetch(getBaseUrl('auth/auth-options'), {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        })
            .then(value => value.json())
            .then(optionsMap => Object.keys(optionsMap).reduce((result, url) => {
                result.push({name: optionsMap[url], url: url});
                return result;
            }, []))
            .then(setAuthOptions)
            .catch(error => {
                setMessageBox({
                    open: true,
                    severity: 'error',
                    title: 'Login failed',
                    message: (error && error.message) || error
                });
            })
    }, []);

    return (
        <Box>
            <div className="AuthContainer">
                {<img src={Logo} alt=''/>}
                <Paper variant="outlined" elevation={1}>
                    <LoginFormComponent/>
                    <Divider variant="middle"/>
                    <OauthLoginComponent authOptions={authOptions}/>
                </Paper>
                <Typography variant="caption">3.5.0</Typography>
                <MessageBox
                    config={messageBox}
                    onClose={() => setMessageBox({...messageBox, open: false})}
                />
            </div>
        </Box>
    );
}
