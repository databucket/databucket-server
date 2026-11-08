import React, {useEffect, useState} from "react";
import "./SignUpPage.css";
import DatabucketLogo from "../utils/DatabucketLogo";
import {Input, InputLabel, Paper, Button, Typography, FormControl, Link} from "@mui/material";
import {MessageBox} from "../utils/MessageBox";
import {Navigate} from "react-router-dom";
import {validateEmail} from "../../utils/Misc";
import {getBaseUrl, getContextPath} from "../../utils/UrlBuilder";
import {handleErrors, handleLoginErrors} from "../../utils/FetchHelper";
import ProjectRequest from "./ProjectRequest";
import PasswordVisibility from "./PasswordVisibility";

export default function SignUpPage() {

    const [back, setBack] = useState(false);
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [passwordConfirmation, setPasswordConfirmation] = useState("");
    // the eye of each password field shows or hides only that field
    const [shown, setShown] = useState({});
    const toggleShown = name => setShown(prev => ({...prev, [name]: !prev[name]}));
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [recaptcha, setRecaptcha] = useState({enabled: true, siteKey: null});
    const [loading, setLoading] = useState(false);
    const [projects, setProjects] = useState([]);
    const [projectsIds, setProjectsIds] = useState([]);
    const [message, setMessage] = useState("");
    // two steps when there are public projects to ask for: 1 the account, 2 the projects (optional)
    const [step, setStep] = useState(1);

    useEffect(() => {
        fetch(getBaseUrl('public/projects'))
            .then(handleLoginErrors)
            .then(setProjects)
            .catch(() => setProjects([]));
    }, []);

    useEffect(() => {
        if (recaptcha.enabled === true && recaptcha.siteKey == null) {
            fetch(getBaseUrl('public/recaptcha-site-key'), {
                method: 'GET',
                headers: {'Content-Type': 'application/json'}
            })
                .then(handleLoginErrors)
                .then(response => {
                    // console.log("Loaded site key: " + response.siteKey);
                    if (response.enabled === true)
                        setRecaptcha({enabled: true, siteKey: response.siteKey});
                    else
                        setRecaptcha({...recaptcha, enabled: false});
                })
                .catch(error => {
                    setMessageBox({open: true, severity: 'error', title: 'Getting site key failed', message: error});
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
                    if (callback) callback();
                };
                document.body.appendChild(script);
            }
            if (isScriptExist && callback) callback();
        }

        // load the script by passing the URL
        if (recaptcha.siteKey != null) {
            loadScriptByURL("recaptcha-key", `https://www.google.com/recaptcha/api.js?render=${recaptcha.siteKey}`, function () {
                // console.log("Recaptcha script loaded with given site key!");
            });
        }
    }, [recaptcha.siteKey]);

    const handleSubmit = e => {
        setLoading(true);
        e?.preventDefault();

        if (recaptcha.enabled) {
            window.grecaptcha.ready(() => {
                window.grecaptcha.execute(recaptcha.siteKey, {action: 'submit'}).then(token => {
                    submitData(token);
                });
            });
        } else
            submitData(null);
    }

    const submitData = token => {
        let errorResp = false;
        const payload = {
            username: username,
            email: email,
            password: password,
            url: window.location.origin + getContextPath() + "/confirmation/sign-up/",
            recaptchaToken: token,
            projectsIds: projectsIds,
            message: message
        }

        fetch(getBaseUrl('public/sign-up'), {
            method: 'POST',
            body: JSON.stringify(payload),
            headers: {'Content-Type': 'application/json'}
        })
            .then(handleErrors)
            .catch(error => {
                errorResp = true;
                setLoading(false);
                // e.g. the username or the email is taken: back to the account
                setStep(1);
                setMessageBox({open: true, severity: 'error', title: 'Registration failed', message: error});
            })
            .then(() => {
                if (!errorResp) {
                    setLoading(false);
                    setMessageBox({open: true, severity: 'info', title: 'Send confirmation email', message: ""});
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
        if (!(username.length >= 3 && username.length <= 50))
            setMessageBox({open: true, severity: 'info', title: 'Please enter a username between 3 and 50 characters.', message: ''});
    }

    const checkEmail = (email) => {
        if (!validateEmail(email))
            setMessageBox({open: true, severity: 'error', title: 'Invalid email address.', message: ''});
    }

    const accountValid = validateEmail(email)
        && username.length >= 3
        && username.length <= 50
        && password.length > 0
        && passwordConfirmation.length > 0
        && password === passwordConfirmation;
    const twoSteps = projects != null && projects.length > 0;

    const handleKeypress = e => {
        if (e.key === 'Enter' && accountValid && !loading) {
            if (twoSteps)
                setStep(2);
            else
                handleSubmit();
        }
    };

    if (back)
        return (<Navigate to="/login"/>);
    else
        return (
            <div className="ContainerClassSingUp">
                <DatabucketLogo/>
                <Paper className="PaperClassSingUp" elevation={3}>
                    <Typography className="TitleSingUp" variant="h5">
                        Sign up
                    </Typography>
                    {twoSteps &&
                        <Typography variant="caption" color="textSecondary">Step {step} of 2</Typography>}
                    {step === 1 && <>
                    <Typography className="DescriptionSingUp">
                        You want to create a new account?
                        Send required fields and wait for the confirmation link.
                        Until you confirm your registration, your account will be inactive.
                    </Typography>
                    <FormControl className="UsernameInputTextSingUp">
                        <InputLabel htmlFor="standard-adornment-username">Username</InputLabel>
                        <Input
                            id="standard-adornment-username"
                            name="username"
                            type='text'
                            value={username}
                            onChange={handleSetUsername}
                            onKeyPress={(event) => handleKeypress(event)}
                            onFocus={(event) => {
                                event.target.setAttribute('autocomplete', 'off');
                            }}
                        />
                    </FormControl>
                    <FormControl className="EmailInputTextSingUp">
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
                    <FormControl className="PasswordTextSingUp">
                        <InputLabel htmlFor="standard-adornment-password">Password</InputLabel>
                        <Input
                            id="standard-adornment-password"
                            name="password"
                            type={shown.password ? 'text' : 'password'}
                            value={password}
                            onChange={handlePassword}
                            endAdornment={<PasswordVisibility shown={shown.password === true} onToggle={() => toggleShown('password')}/>}
                            onKeyPress={(event) => handleKeypress(event)}
                            onFocus={(event) => {
                                event.target.setAttribute('autocomplete', 'off');
                            }}
                        />
                    </FormControl>
                    <FormControl className="PasswordTextSingUp">
                        <InputLabel htmlFor="standard-adornment-confirm-password">Confirm password</InputLabel>
                        <Input
                            name="passwordConfirmation"
                            type={shown.passwordConfirmation ? 'text' : 'password'}
                            value={passwordConfirmation}
                            onChange={handlePasswordConfirmation}
                            endAdornment={<PasswordVisibility shown={shown.passwordConfirmation === true} onToggle={() => toggleShown('passwordConfirmation')}/>}
                            onKeyPress={(event) => handleKeypress(event)}
                            onFocus={(event) => {
                                event.target.setAttribute('autocomplete', 'off');
                            }}
                        />
                    </FormControl>
                    </>}
                    {step === 2 && <>
                        <Typography className="DescriptionSingUp">
                            Optionally, choose the projects you want to work in and write a message for their
                            administrators. You can also submit without choosing any.
                        </Typography>
                        <ProjectRequest projects={projects} selected={projectsIds} onChange={setProjectsIds}
                                        message={message} onMessageChange={setMessage}/>
                    </>}
                    <div className="ButtonSingUp">
                        {step === 1 && twoSteps
                            ? <Button fullWidth={true} variant="contained" color="primary" size={'large'}
                                      disabled={!accountValid} onClick={() => setStep(2)}>
                                Next
                            </Button>
                            : <Button fullWidth={true} variant="contained" color="primary" size={'large'}
                                      disabled={!accountValid || loading} onClick={handleSubmit}>
                                {loading ? 'Processing...' : 'Submit'}
                            </Button>}
                    </div>
                    <div className="BackLinkSingUp">
                        <Link
                            component="button"
                            color="inherit"
                            disabled={loading}
                            onClick={() => step === 2 ? setStep(1) : setBack(true)}
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