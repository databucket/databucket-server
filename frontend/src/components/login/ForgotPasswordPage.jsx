import React, {useState} from "react";
import "./ForgotPasswordPage.css";
import Button from "@material-ui/core/Button";
import Logo from "../../images/databucket-logo.png";
import {Input, InputLabel, Paper} from "@material-ui/core";
import Typography from "@material-ui/core/Typography";
import FormControl from "@material-ui/core/FormControl";
import {MessageBox} from "../utils/MessageBox";
import Link from "@material-ui/core/Link";
import {Redirect} from "react-router-dom";
import {getBaseUrl, getContextPath} from "../../utils/UrlBuilder";
import {handleLoginErrors} from "../../utils/FetchHelper";
import {validateEmail} from "../../utils/Misc";

export default function ForgotPasswordPage() {

    const [back, setBack] = useState(false);
    const [email, setEmail] = useState("");
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});

    const onChange = e => {
        setEmail(e.target.value);
    };

    const handleReset = () => {
        fetch(getBaseUrl('public/forgot-password'), {
            method: 'POST',
            body: JSON.stringify({email: email, url: window.location.origin + getContextPath() + "/confirmation/forgot-password/"}),
            headers: {'Content-Type': 'application/json'}
        })
            .then(handleLoginErrors)
            .then(response => {
                setMessageBox({open: true, severity: 'info', title: 'Send confirmation email', message: null});
            }).catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Sending confirmation password failed', message: error});
            }
        );
    }

    const handleKeypress = e => {
        if (e.key === 'Enter')
            handleReset();
    };

    if (back)
        return (<Redirect to="/login"/>);
    else
        return (
            <div className="ContainerClass">
                {<img src={Logo} alt=''/>}
                <Paper className="PaperClass" elevation={3}>
                    <Typography className="Title" variant="h5">
                        Forgot your password?
                    </Typography>
                    <Typography className="Description">
                        Please enter the email address for your account.
                        A verification link will be sent to you.
                        Once you have received the verification link,
                        you will be able to choose a new password for your account.
                    </Typography>
                    <FormControl className="EmailInputText">
                        <InputLabel htmlFor="standard-adornment-email">Email</InputLabel>
                        <Input
                            id="standard-adornment-email"
                            name="email"
                            type='email'
                            value={email}
                            onChange={onChange}
                            onKeyPress={(event) => handleKeypress(event)}
                        />
                    </FormControl>
                    <div className="Button">
                        <Button
                            fullWidth={true}
                            variant="contained"
                            color="primary"
                            size={'large'}
                            disabled={!(validateEmail(email))}
                            onClick={() => {
                                handleReset();
                            }}
                        >
                            Submit
                        </Button>
                    </div>
                    <div className="BackLink">
                        <Link
                            component="button"
                            color="inherit"
                            onClick={() => {
                                setBack(true);
                            }}
                        >
                            Back
                        </Link>
                    </div>
                </Paper>
                <MessageBox
                    config={messageBox}
                    onClose={() => setMessageBox({...messageBox, open: false})}
                />
            </div>
        );
}