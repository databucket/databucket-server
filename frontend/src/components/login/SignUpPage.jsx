import React, {useState} from "react";
import "./SignUpPage.css";
import Button from "@material-ui/core/Button";
import Logo from "../../images/databucket-logo.png";
import {Input, InputLabel, Paper} from "@material-ui/core";
import Typography from "@material-ui/core/Typography";
import FormControl from "@material-ui/core/FormControl";
import {MessageBox} from "../utils/MessageBox";
import Link from "@material-ui/core/Link";
import {Redirect} from "react-router-dom";
import {validateEmail} from "../../utils/Misc";
import {getBaseUrl, getContextPath} from "../../utils/UrlBuilder";
import {handleLoginErrors} from "../../utils/FetchHelper";

export default function SignUpPage() {

    const [back, setBack] = useState(false);
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [passwordConfirmation, setPasswordConfirmation] = useState("");
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});

    const handleSetUsername = e => {
        setUsername(e.target.value);
        checkUsername(e.target.value);
    };

    const handleSetEmail = e => {
        setEmail(e.target.value);
        checkEmail(e.target.value);
    };

    const handlePassword = e => {
        setPassword(e.target.value);
        getPasswordStrength(e.target.value);
    };

    const handlePasswordConfirmation = e => {
        setPasswordConfirmation(e.target.value);
        checkPasswordConfirmation(e.target.value);
    };

    const handleSubmit = () => {
        fetch(getBaseUrl('public/sign-up'), {
            method: 'POST',
            body: JSON.stringify({username: username, email: email, password: password, url: window.location.origin + getContextPath() + "/confirmation/sign-up/"}),
            headers: {'Content-Type': 'application/json'}
        })
            .then(handleLoginErrors)
            .then(response => {
                setMessageBox({open: true, severity: 'info', title: 'Send confirmation email', message: null});
            }).catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Registration failed', message: error});
            }
        );
    }

    const getPasswordStrength = (pwd) => {
        const strongRegex = new RegExp("^(?=.{14,})(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*\\W).*$", "g");
        const mediumRegex = new RegExp("^(?=.{10,})(((?=.*[A-Z])(?=.*[a-z]))|((?=.*[A-Z])(?=.*[0-9]))|((?=.*[a-z])(?=.*[0-9]))).*$", "g");
        const enoughRegex = new RegExp("(?=.{8,}).*", "g");

        if (strongRegex.test(pwd)) {
            setMessageBox({open: true, severity: 'success', title: 'Strong password.', message: ''});
        } else if (mediumRegex.test(pwd)) {
            setMessageBox({open: true, severity: 'info', title: 'Medium password.', message: ''});
        } else if (enoughRegex.test(pwd)) {
            setMessageBox({open: true, severity: 'warning', title: 'Weak password!', message: ''});
        } else {
            setMessageBox({open: true, severity: 'error', title: 'Very weak password!!!', message: ''});
        }
    }

    const checkPasswordConfirmation = (passwordConfirmation) => {
        if (passwordConfirmation !== password)
            setMessageBox({open: true, severity: 'error', title: 'Your password and confirmation password do not match.', message: ''});
        else
            setMessageBox({open: true, severity: 'success', title: 'Your password and confirmation password match.', message: ''});
    }

    const checkUsername = (username) => {
        if (!(username.length >=3 && username.length < 30))
            setMessageBox({open: true, severity: 'info', title: 'Please enter a username between 3 and 30 characters.', message: ''});
    }

    const checkEmail = (email) => {
        if (!validateEmail(email))
            setMessageBox({open: true, severity: 'error', title: 'Invalid email address.', message: ''});
    }

    const handleKeypress = e => {
        if (e.key === 'Enter')
            handleSubmit();
    };

    if (back)
        return (<Redirect to="/login"/>);
    else
        return (
            <div className="ContainerClass">
                {<img src={Logo} alt=''/>}
                <Paper className="PaperClass" elevation={3}>
                    <Typography className="Title" variant="h5">
                        Sign up
                    </Typography>
                    <Typography className="Description">
                        You want to create a new account?
                        Send required fields and wait for the confirmation link.
                        Until you confirm your registration, your account will be inactive.
                    </Typography>
                    {/*// additional control to block setting default password by Chrome*/}
                    <FormControl disabled={true} style={{height: '0px', width: '0px'}}>
                        <Input
                            name="pass"
                            type='password'
                            value={null}
                            disabled={true}
                        />
                    </FormControl>
                    <FormControl className="UsernameInputText">
                        <InputLabel htmlFor="standard-adornment-username">Username</InputLabel>
                        <Input
                            id="standard-adornment-username"
                            name="username"
                            type='text'
                            value={username}
                            onChange={handleSetUsername}
                            onKeyPress={(event) => handleKeypress(event)}
                        />
                    </FormControl>
                    <FormControl className="EmailInputText">
                        <InputLabel htmlFor="standard-adornment-email">Email</InputLabel>
                        <Input
                            id="standard-adornment-email"
                            name="email"
                            type="email"
                            value={email}
                            onChange={handleSetEmail}
                            onKeyPress={(event) => handleKeypress(event)}
                        />
                    </FormControl>
                    <FormControl className="PasswordText">
                        <InputLabel htmlFor="standard-adornment-password">Password</InputLabel>
                        <Input
                            id="standard-adornment-password"
                            name="password"
                            type={'password'}
                            value={password}
                            onChange={handlePassword}
                        />
                    </FormControl>
                    <FormControl className="PasswordText">
                        <InputLabel htmlFor="standard-adornment-confirm-password">Confirm password</InputLabel>
                        <Input
                            name="passwordConfirmation"
                            type='password'
                            value={passwordConfirmation}
                            onChange={handlePasswordConfirmation}
                        />
                    </FormControl>
                    <div className="Button">
                        <Button
                            fullWidth={true}
                            variant="contained"
                            color="primary"
                            size={'large'}
                            disabled={
                                !(validateEmail(email)
                                    && username.length >= 3
                                    && password.length > 0
                                    && passwordConfirmation.length > 0
                                    && password === passwordConfirmation
                                )}
                            onClick={() => {
                                handleSubmit();
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