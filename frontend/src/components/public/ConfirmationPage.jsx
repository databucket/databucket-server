import React, {useEffect, useState} from "react";
import Logo from "../../images/databucket-logo.png";
import {
    Paper as RawPaper,
    Stack as RawStack,
    Typography
} from "@mui/material";
import {MessageBox} from "../utils/MessageBox";
import {Navigate, useParams} from "react-router-dom";
import {getConfirmationUrl} from "../../utils/UrlBuilder";
import {handleErrors} from "../../utils/FetchHelper";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import styled from "@emotion/styled";

const Paper = styled(RawPaper)`
  min-width: 20vw;
  max-width: 50vw;
`;
const Stack = styled(RawStack)`
  min-width: 30vw;
`;

export default function ConfirmationPage() {

    const inputParams = useParams();
    const [messageBox, setMessageBox] = useState(
        {open: false, severity: 'error', title: '', message: ''});
    const [done, setDone] = useState(false);
    const [failed, setFailed] = useState(false);
    const [redirect, setRedirect] = useState(false);

    useEffect(() => {
        let resultOk = true;
        // the rest of the path after /confirmation/, e.g. "sign-up/<token>" (route "/confirmation/*")
        fetch(getConfirmationUrl(inputParams['*']), getGetOptions())
        .then(handleErrors)
        .catch(error => {
            setMessageBox({
                open: true,
                severity: 'error',
                title: 'Error',
                // handleErrors rejects with the server's message; anything else (e.g. a network error) is an Error
                message: typeof error === 'string' ? error : error.message
            });
            setFailed(true);
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
        return (<Navigate to="/login"/>);
    } else {
        return (
            <Stack direction="column"
                   alignItems="center"
                   spacing={2}
            >
                {<img src={Logo} alt=''/>}
                <Paper elevation={3}>
                    <Stack direction="column"
                           spacing={2}
                           alignItems="center"
                           p={3}
                    >
                        <Typography className="Title" variant="h5">
                            Confirmation
                        </Typography>
                        <div style={{height: '100px'}}/>
                        <Typography className="Description">
                            {failed ? "The confirmation failed."
                                : !done ? "Processing..."
                                : "Check your email inbox."}
                        </Typography>
                    </Stack>
                </Paper>
                <MessageBox
                    config={messageBox}
                    onClose={() => setMessageBox({...messageBox, open: false})}
                />
            </Stack>
        );
    }
}
