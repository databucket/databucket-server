import {Button, FormControl, Input, InputLabel, Link as MaterialLink, Paper, Typography} from "@material-ui/core";
import React, {forwardRef, useState} from "react";
import {MessageBox} from "../utils/MessageBox";
import {clearToken, getToken, getUsername} from "../../utils/ConfigurationStorage";
import {fetchHelper, handleErrors} from "../../utils/FetchHelper";
import {Link, Redirect} from "react-router-dom";
import {getProjectDataPath} from "../../route/AppRouter";
import {getBaseUrl} from "../../utils/UrlBuilder";

const initialState = {
    password: "",
    newPassword: "",
    newPasswordConfirmation: ""
};

const FancyLink = forwardRef(({navigate, ...props}, ref) => {
    return (
        <MaterialLink
            ref={ref}
            variant="caption"
            color="inherit"
            {...props}
        >{props.children}</MaterialLink>
    )
});
export default function ChangePasswordPage() {
    const [{password, newPassword, newPasswordConfirmation}, setState] = useState(initialState);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [redirect, setRedirect] = useState(false);

    const handleChangePassword = () => {
        const username = getUsername();
        if (newPassword === newPasswordConfirmation) {
            fetch(getBaseUrl('users/password/change'), {
                method: 'POST',
                body: JSON.stringify({username, password, newPassword}),
                headers: fetchHelper(getToken())
            })
                .then(handleErrors)
                .then(() => {
                    clearToken();
                    setMessageBox({
                        open: true,
                        severity: 'success',
                        title: 'Success',
                        message: 'The password has been changed'
                    });
                    setTimeout(() => {
                        setRedirect(true);
                    }, 1000)
                }).catch(error => {
                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                }
            );
        } else
            setMessageBox({
                open: true,
                severity: 'info',
                title: 'Password confirmation must match new password',
                message: ''
            });
    }

    const getPasswordStrength = (pwd) => {
        const strongRegex = /^(?=.{14,})(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*\W).*$/g;
        const mediumRegex = /^(?=.{10,})(((?=.*[A-Z])(?=.*[a-z]))|((?=.*[A-Z])(?=.*\d))|((?=.*[a-z])(?=.*\d))).*$/g;
        const enoughRegex = /(?=.{8,}).*/g;

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

    const onChange = e => {
        const {name, value} = e.target;
        setState(prevState => ({...prevState, [name]: value}));

        if (name === 'newPassword')
            getPasswordStrength(value);
    };

    return (
        redirect === true ? (
            <Redirect to={getProjectDataPath()}/>
        ) : (
            <div className="ContainerClass">
                <Paper className="PaperClass" elevation={3}>
                    <Typography className="Title" variant="h5">
                        Change password
                    </Typography>
                    <FormControl className="LoginInputText">
                        <InputLabel htmlFor="standard-adornment-password">Current password</InputLabel>
                        <Input
                            name="password"
                            type='password'
                            value={password}
                            onChange={onChange}
                            onFocus={(event) => {
                                event.target.setAttribute('autocomplete', 'off');
                            }}
                        />
                    </FormControl>
                    <FormControl className="LoginInputText">
                        <InputLabel htmlFor="standard-adornment-new-password">New password</InputLabel>
                        <Input
                            name="newPassword"
                            type='password'
                            value={newPassword}
                            onChange={onChange}
                            onFocus={(event) => {
                                event.target.setAttribute('autocomplete', 'off');
                            }}
                        />
                    </FormControl>
                    <FormControl className="LoginInputText">
                        <InputLabel htmlFor="standard-adornment-confirm-password">Confirm new password</InputLabel>
                        <Input
                            name="newPasswordConfirmation"
                            type='password'
                            value={newPasswordConfirmation}
                            onChange={onChange}
                            onFocus={(event) => {
                                event.target.setAttribute('autocomplete', 'off');
                            }}
                        />
                    </FormControl>
                    <div className="Button">
                        <Button
                            fullWidth={true}
                            variant="contained"
                            color="primary"
                            disabled={
                                !(password.length > 0
                                    && newPassword.length > 0
                                    && newPasswordConfirmation.length > 0
                                )}
                            onClick={handleChangePassword}
                        >
                            Submit
                        </Button>
                    </div>
                    <div className="BackLink">
                        <Link to="/login-form" component={FancyLink}>Back</Link>
                    </div>
                </Paper>
                <MessageBox
                    config={messageBox}
                    onClose={() => setMessageBox({...messageBox, open: false})}
                />
            </div>
        )
    );
}
