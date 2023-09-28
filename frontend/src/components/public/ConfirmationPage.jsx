import React, {useEffect, useState} from "react";
import "./SignUpPage.css";
import Logo from "../../images/databucket-logo.png";
import {Paper, Typography} from "@mui/material";
import {MessageBox} from "../utils/MessageBox";
import {Redirect, useParams} from "react-router-dom";
import {getConfirmationUrl} from "../../utils/UrlBuilder";
import {handleErrors} from "../../utils/FetchHelper";
import {getGetOptions} from "../../utils/MaterialTableHelper";

export default function ConfirmationPage() {

    const inputParams = useParams();
    const [messageBox, setMessageBox] = useState(
        {open: false, severity: 'error', title: '', message: ''});
    const [done, setDone] = useState(false);
    const [redirect, setRedirect] = useState(false);

    useEffect(() => {
        let resultOk = true;
        fetch(getConfirmationUrl(inputParams[0]), getGetOptions())
        .then(handleErrors)
        .catch(error => {
            setMessageBox({
                open: true,
                severity: 'error',
                title: 'Error',
                message: error
            });
            resultOk = false;
        })
        .then(response => {
            if (resultOk) {
                setDone(true);
                setTimeout(() => {
                    setRedirect(true);
                }, 6000)
            }
        });
    }, [inputParams]);

    if (redirect) {
        return (<Redirect to="/login-form"/>);
    } else {
        return (
            <div className="ContainerClass">
                {<img src={Logo} alt=''/>}
                <Paper className="PaperClass" elevation={3}>
                    <Typography className="Title" variant="h5">
                        Confirmation
                    </Typography>
                    <div style={{height: '100px'}}/>
                    <Typography className="Description">
                        {!done ? "Processing..." : "Check your email inbox."}
                    </Typography>
                </Paper>
                <MessageBox
                    config={messageBox}
                    onClose={() => setMessageBox({...messageBox, open: false})}
                />
            </div>
        );
    }
}
