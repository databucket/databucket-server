import React, {useEffect, useState} from "react";
import "./LoginPage.css";
import Logo from "../../images/databucket-logo.png";
import Typography from "@material-ui/core/Typography";
import {Redirect} from 'react-router-dom';
import {MessageBox} from "../utils/MessageBox";
import {getBaseUrl} from "../../utils/UrlBuilder";
import OauthLoginComponent from "../auth/OauthLogin";
import Paper from "@material-ui/core/Paper";

export default function AuthPage() {

    const [authOptions, setAuthOptions] = useState([]);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    useEffect(() => {
        fetch(getBaseUrl('public/auth-options'), {
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
                setMessageBox({open: true, severity: 'error', title: 'Login failed', message: error});
            })
    }, []);
    const handleSignIn = () => {
        // signIn(username, password, null);
    }

    const handleKeypress = e => {
        if (e.key === 'Enter')
            handleSignIn();
    };

    const redirectTo = (pagePath) => {
        return (<Redirect to={pagePath}/>);
    }

    return (
        <div className="ContainerClass">
            {<img src={Logo} alt=''/>}
            <Paper variant="outlined">
                <OauthLoginComponent authOptions={authOptions}/>
            </Paper>
            <Typography variant="caption">3.4.4</Typography>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}
