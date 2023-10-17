import React, {useEffect, useState} from "react";
import Logo from "../../images/databucket-logo.png";
import {
    Button,
    FormControl,
    IconButton,
    Input,
    InputAdornment,
    InputLabel,
    Link as RawLink,
    Paper as RawPaper,
    Stack as RawStack,
    TextField,
    Typography
} from "@mui/material";
import {MessageBox} from "../utils/MessageBox";
import {Link as RouterLink, Redirect} from "react-router-dom";
import {validateEmail} from "../../utils/Misc";
import {getBaseUrl, getContextPath} from "../../utils/UrlBuilder";
import {handleErrors, handleLoginErrors} from "../../utils/FetchHelper";
import {Visibility, VisibilityOff} from "@mui/icons-material";
import styled from "@emotion/styled";

const Paper = styled(RawPaper)`
  min-width: 20vw;
  max-width: 50vw;
`;
const Stack = styled(RawStack)`
  min-width: 30vw;
`;

const Link = styled(RawLink)`
  width: 100%;
  margin-top: 20px;
  margin-left: 40px;
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
`;

export default function SignUpPage() {

    const [back, setBack] = useState(false);
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [passwordConfirmation, setPasswordConfirmation] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [messageBox, setMessageBox] = useState(
        {open: false, severity: 'error', title: '', message: ''});
    const [recaptcha, setRecaptcha] = useState({enabled: true, siteKey: null});
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (recaptcha.enabled === true && recaptcha.siteKey == null) {
            fetch(getBaseUrl('auth/recaptcha-site-key'), {
                method: 'GET',
                headers: {'Content-Type': 'application/json'}
            })
            .then(handleLoginErrors)
            .then(response => {
                // console.log("Loaded site key: " + response.siteKey);
                if (response.enabled === true) {
                    setRecaptcha({enabled: true, siteKey: response.siteKey});
                } else {
                    setRecaptcha({...recaptcha, enabled: false});
                }
            })
            .catch(error => {
                setMessageBox({
                    open: true,
                    severity: 'error',
                    title: 'Getting site key failed',
                    message: error
                });
            });
        }
    }, []);

    useEffect(() => {
        const loadScriptByURL = (id, url, callback) => {
            const isScriptExist = document.getElementById(id);

            if (!isScriptExist) {
                let script = document.createElement("script");
                script.type = "text/javascript";
                script.src = url;
                script.id = id;
                script.onload = function () {
                    if (callback) {
                        callback();
                    }
                };
                document.body.appendChild(script);
            }
            if (isScriptExist && callback) {
                callback();
            }
        }

        // load the script by passing the URL
        if (recaptcha.siteKey != null) {
            loadScriptByURL("recaptcha-key",
                `https://www.google.com/recaptcha/api.js?render=${recaptcha.siteKey}`,
                function () {
                    // console.log("Recaptcha script loaded with given site key!");
                });
        }
    }, [recaptcha.siteKey]);

    const handleSubmit = e => {
        setLoading(true);
        e.preventDefault();

        if (recaptcha.enabled) {
            window.grecaptcha.ready(() => {
                window.grecaptcha.execute(recaptcha.siteKey,
                    {action: 'submit'}).then(token => {
                    submitData(token);
                });
            });
        } else {
            submitData(null);
        }
    }

    const submitData = token => {
        let errorResp = false;
        const payload = {
            username: username,
            email: email,
            password: password,
            url: window.location.origin + getContextPath()
                + "/confirmation/sign-up/",
            recaptchaToken: token
        }

        fetch(getBaseUrl('auth/sign-up'), {
            method: 'POST',
            body: JSON.stringify(payload),
            headers: {'Content-Type': 'application/json'}
        })
        .then(handleErrors)
        .catch(error => {
            errorResp = true;
            setLoading(false);
            setMessageBox({
                open: true,
                severity: 'error',
                title: 'Registration failed',
                message: error
            });
        })
        .then(() => {
            if (!errorResp) {
                setLoading(false);
                setMessageBox({
                    open: true,
                    severity: 'info',
                    title: 'Send confirmation email',
                    message: ""
                });
                setTimeout(() => {
                    setBack(true);
                }, 6000)
            }
        });
    }

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

    const getPasswordStrength = (pwd) => {
        const strongRegex = new RegExp(
            "^(?=.{14,})(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*\\W).*$", "g");
        const mediumRegex = new RegExp(
            "^(?=.{10,})(((?=.*[A-Z])(?=.*[a-z]))|((?=.*[A-Z])(?=.*[0-9]))|((?=.*[a-z])(?=.*[0-9]))).*$",
            "g");
        const enoughRegex = new RegExp("(?=.{8,}).*", "g");

        if (strongRegex.test(pwd)) {
            setMessageBox({
                open: true,
                severity: 'success',
                title: 'Strong password.',
                message: ''
            });
        } else if (mediumRegex.test(pwd)) {
            setMessageBox({
                open: true,
                severity: 'info',
                title: 'Medium password.',
                message: ''
            });
        } else if (enoughRegex.test(pwd)) {
            setMessageBox({
                open: true,
                severity: 'warning',
                title: 'Weak password!',
                message: ''
            });
        } else {
            setMessageBox({
                open: true,
                severity: 'error',
                title: 'Very weak password!!!',
                message: ''
            });
        }
    }

    const checkPasswordConfirmation = (passwordConfirmation) => {
        if (passwordConfirmation !== password) {
            setMessageBox({
                open: true,
                severity: 'error',
                title: 'Your password and confirmation password do not match.',
                message: ''
            });
        } else {
            setMessageBox({
                open: true,
                severity: 'success',
                title: 'Your password and confirmation password match.',
                message: ''
            });
        }
    }

    const checkUsername = (username) => {
        if (!(username.length >= 3 && username.length < 30)) {
            setMessageBox({
                open: true,
                severity: 'info',
                title: 'Please enter a username between 3 and 30 characters.',
                message: ''
            });
        }
    }

    const checkEmail = (email) => {
        if (!validateEmail(email)) {
            setMessageBox({
                open: true,
                severity: 'error',
                title: 'Invalid email address.',
                message: ''
            });
        }
    }

    const handleKeypress = e => {
        if (e.key === 'Enter') {
            handleSubmit();
        }
    };

    const handleClickShowPassword = () => {
        setShowPassword(!showPassword);
    }

    const handleMouseDownPassword = (event) => {
        event.preventDefault();
    };

    if (back) {
        return (<Redirect to="/login-form"/>);
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
                           component="form"
                           noValidate
                           onSubmit={handleSubmit}
                           p={3}
                    >
                        <Typography variant="h5" p={3}>
                            Sign up
                        </Typography>
                        <Typography sx={{maxWidth: "48vh"}}>
                            You want to create a new account?<br/>
                            Send required fields and wait for the confirmation
                            link.
                            Until you confirm your registration, your account
                            will be inactive.
                        </Typography>
                        <TextField
                            fullWidth
                            variant="standard"
                            id="standard-adornment-username"
                            name="username"
                            type='text'
                            label="Username"
                            value={username}
                            onChange={handleSetUsername}
                            onKeyDown={handleKeypress}
                            onFocus={(event) => {
                                event.target.setAttribute('autocomplete',
                                    'off');
                            }}
                        />
                        <TextField
                            fullWidth
                            variant="standard"
                            id="standard-adornment-email"
                            name="email"
                            type="email"
                            label="Email"
                            value={email}
                            onChange={handleSetEmail}
                            onKeyDown={handleKeypress}
                        />
                        <FormControl variant="standard" fullWidth>
                            <InputLabel
                                htmlFor="standard-adornment-password">Password</InputLabel>
                            <Input
                                id="standard-adornment-password"
                                name="password"
                                autoComplete="current-password"
                                type={showPassword ? 'text' : 'password'}
                                value={password}
                                onChange={handlePassword}
                                onFocus={(event) => {
                                    event.target.setAttribute('autocomplete',
                                        'off');
                                }}
                                endAdornment={
                                    <InputAdornment position="end">
                                        <IconButton
                                            aria-label="toggle password visibility"
                                            onClick={handleClickShowPassword}
                                            onMouseDown={handleMouseDownPassword}
                                        >
                                            {showPassword ? <Visibility/> :
                                                <VisibilityOff/>}
                                        </IconButton>
                                    </InputAdornment>
                                }
                            />
                        </FormControl>
                        <FormControl variant="standard" fullWidth>
                            <InputLabel
                                htmlFor="standard-adornment-confirm-password">Confirm
                                password</InputLabel>
                            <Input
                                id="standard-adornment-password-confirmation"
                                name="passwordConfirmation"
                                autoComplete="new-password"
                                type={showPassword ? 'text' : 'password'}
                                value={passwordConfirmation}
                                onChange={handlePasswordConfirmation}
                                onFocus={(event) => {
                                    event.target.setAttribute('autocomplete',
                                        'off');
                                }}
                                endAdornment={
                                    <InputAdornment position="end">
                                        <IconButton
                                            aria-label="toggle password visibility"
                                            onClick={handleClickShowPassword}
                                            onMouseDown={handleMouseDownPassword}
                                        >
                                            {showPassword ? <Visibility/> :
                                                <VisibilityOff/>}
                                        </IconButton>
                                    </InputAdornment>
                                }
                            />
                        </FormControl>
                        <Button
                            type="submit"
                            fullWidth
                            variant="contained"
                            color="primary"
                            size="large"
                            disabled={
                                !(validateEmail(email)
                                    && username.length >= 3
                                    && password.length > 0
                                    && passwordConfirmation.length > 0
                                    && password === passwordConfirmation
                                    && !loading
                                )}
                        >
                            {loading ? 'Processing...' : 'Submit'}
                        </Button>
                        <Link
                            component={RouterLink}
                            color="inherit"
                            to="/login-form"
                            underline="hover"
                        >
                            Back
                        </Link>
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
