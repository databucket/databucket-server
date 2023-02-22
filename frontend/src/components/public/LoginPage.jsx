import React, {useState} from "react";
import "./LoginPage.css";
import Button from "@mui/material/Button";
import Logo from "../../images/databucket-logo.png";
import {Input, InputAdornment, InputLabel, Paper} from "@material-ui/core";
import Typography from "@material-ui/core/Typography";
import {getPathname, hasProject, hasSuperRole, hasToken, setPathname} from '../../utils/ConfigurationStorage';
import {Redirect} from 'react-router-dom';
import FormControl from "@material-ui/core/FormControl";
import IconButton from "@material-ui/core/IconButton";
import {Visibility, VisibilityOff} from "@material-ui/icons";
import {MessageBox} from "../utils/MessageBox";
import {getManagementProjectsPath, getProjectDataPath} from "../../route/AppRouter";
import MaterialLink from "@material-ui/core/Link";
import {signIn} from "../utils/AuthHelper";

const initialState = {
    username: "",
    password: "",
    projects: null,
    resetPassword: false,
    changePassword: false,
    register: false,
    showPassword: false
};

export default function LoginPage() {

    const [state, setState] = useState(initialState);
    const {
        username,
        password,
        projects,
        resetPassword,
        changePassword,
        register,
        showPassword
    } = state;
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});

    const onChange = e => {
        const {name, value} = e.target;
        setState(prevState => ({...prevState, [name]: value}));
    };

    const handleClickShowPassword = () => {
        setState(prevState => ({...prevState, showPassword: !showPassword}));
    }

    const handleMouseDownPassword = (event) => {
        event.preventDefault();
    };

    const handleSignIn = () => {
        signIn(username, password, null)
            .then(value => setState(prevState => ({...prevState, ...value})))
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Login failed', message: error.message});
            });
    }

    const handleKeypress = e => {
        if (e.key === 'Enter')
            handleSignIn();
    };


    const selectProject = (id) => {
        signIn({username, password, projectId: id, ...state})
            .then(value => setState(prevState => ({...prevState, ...value})))
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Login failed', message: error.message});
            });
    }

    const getLoginPaper = () => {
        return (
            <Paper className="PaperClass" elevation={3}>
                <Typography className="Title" variant="h5">
                    Login
                </Typography>
                <FormControl className="LoginInputText">
                    <InputLabel htmlFor="standard-adornment-username">Username</InputLabel>
                    <Input
                        id="standard-adornment-username"
                        name="username"
                        type='text'
                        value={username}
                        onChange={onChange}
                        onKeyPress={(event) => handleKeypress(event)}
                    />
                </FormControl>
                <FormControl className="LoginInputText">
                    <InputLabel htmlFor="standard-adornment-password">Password</InputLabel>
                    <Input
                        id="standard-adornment-password"
                        name="password"
                        // inputProps={{ style: { backgroundColor: "red" } }} //TODO nie działa tylko dla Chrome
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={onChange}
                        onKeyPress={(event) => handleKeypress(event)}
                        endAdornment={
                            <InputAdornment position="end">
                                <IconButton
                                    aria-label="toggle password visibility"
                                    onClick={handleClickShowPassword}
                                    onMouseDown={handleMouseDownPassword}
                                    size="large">
                                    {showPassword ? <Visibility/> : <VisibilityOff/>}
                                </IconButton>
                            </InputAdornment>
                        }
                    />
                </FormControl>
                <MaterialLink
                    component="button"
                    variant="caption"
                    color="inherit"
                    onClick={() => {
                        setState(prevState => ({...prevState, resetPassword: true}));
                    }}
                >
                    Forgot your password?
                </MaterialLink>
                <div className="ButtonLogin">
                    <Button
                        fullWidth={true}
                        variant="contained"
                        color="primary"
                        size={'large'}
                        disabled={!(username.length > 0 && password.length > 0)}
                        onClick={() => {
                            handleSignIn();
                        }}
                    >
                        Login
                    </Button>
                </div>
                <div className="RegistrationLink">
                    <MaterialLink
                        component="button"
                        color="inherit"
                        onClick={() => {
                            setState(prevState => ({...prevState, register: true}));
                        }}
                    >
                        Don't have an account?
                    </MaterialLink>
                </div>
            </Paper>
        );
    }

    const redirectTo = (pagePath) => {
        return (<Redirect to={pagePath}/>);
    }

    const getSwitchParam = () => {
        if (register === true) {
            return 6;
        } else if (resetPassword === true) {
            return 5;
        } else if (changePassword === true) {
            return 4;
        } else if (projects != null && projects.length > 0) {
            return 3;
        } else if (hasToken() && hasProject()) {
            return 2;
        } else if (hasToken() && hasSuperRole()) {
            return 1;
        } else {
            return 0;
        }
    }

    const paper = () => {
        switch (getSwitchParam()) {
            case 6:
                return redirectTo("/sign-up");
            case 5:
                return redirectTo("/forgot-password");
            case 4:
                return redirectTo("/change-password");
            case 3:
                return (<Redirect
                        to={{
                            pathname: "/select-project",
                            state: {projects}
                        }}/>);
            // SelectProjectsPage({projects, selectProject})
            case 2:
                const pathname = getPathname();
                if (pathname != null
                    && pathname !== "null"
                    && !pathname.includes("confirmation")
                    && !pathname.includes("forgot-password")
                    && !pathname.includes("sign-up")
                ) {
                    setPathname(null);
                    return redirectTo(pathname)
                } else {
                    const pathname = getProjectDataPath();
                    return redirectTo(pathname);
                }
            case 1:
                return redirectTo(getManagementProjectsPath());
            default:
                return getLoginPaper();
        }
    }

    return (
        <div className="ContainerClass">
            {<img src={Logo} alt=''/>}
            {paper()}
            <Typography variant="caption">3.5.0</Typography>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}
