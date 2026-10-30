import React, {forwardRef, useEffect, useState} from "react";
import "./LoginPage.css";
import {
    Button, Avatar, Input, InputAdornment, InputLabel, List, ListItem, ListItemAvatar, ListItemText,
    Paper, Tooltip, IconButton, FormControl, Typography, Grid, Divider, Link as MaterialLink
} from "@mui/material";
import Logo from "../../images/databucket-logo.png";
import ActiveProjectIcon from "@mui/icons-material/FolderSpecial";
import DisabledProjectIcon from "@mui/icons-material/FolderSpecial";
import ExpiredProjectIcon from "@mui/icons-material/EventBusy";
import {handleLoginErrors} from "../../utils/FetchHelper";
import {
    setToken,
    setActiveProjectId,
    setRoles, setUsername, hasSuperRole, hasMemberRole, hasAdminRole, hasToken, hasProject, logOut, setSsoLogin, getPathname, setPathname
} from '../../utils/ConfigurationStorage';
import {Navigate, useNavigate, useSearchParams} from 'react-router-dom';
import {Visibility, VisibilityOff} from "@mui/icons-material";
import {MessageBox} from "../utils/MessageBox";
import {getManagementProjectsPath, getProjectDataPath} from "../../route/AppRouter";
import {getBaseUrl} from "../../utils/UrlBuilder";
import ReactGA from 'react-ga';
import {Link} from "react-router-dom";
import {version} from "../../../package.json";
import SsoButton from "./SsoButton";

const initialState = {
    username: "",
    password: "",
    projects: null,
    resetPassword: false,
    changePassword: false,
    register: false,
    showPassword: false
};

// reasons sent by the server as ?sso_error=
const SSO_ERRORS = {
    no_email: 'The identity provider did not send your email address.',
    email_not_verified: 'Your email address is not verified at the identity provider.',
    unknown_user: 'There is no Databucket account for your email address. Ask your administrator to create one.',
    user_disabled: 'Your Databucket account is disabled.',
    sso_unavailable: 'Single sign-on is not available at the moment. Try again later or sign in with your password.',
    idp_error: 'Signing in with the identity provider failed. Please try again.'
};

const FancyLink = forwardRef((props, ref) => {
    return (
        <MaterialLink
            ref={ref}
            component={Link}
            to={props.to}
            color="inherit"
            variant="caption"
            underline="hover"
            mb={2}
        >
            {props.children}
        </MaterialLink>
    )
});

export default function LoginPage() {

    const [{username, password, projects, resetPassword, changePassword, register, showPassword}, setState] = useState(initialState);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [ssoConfig, setSsoConfig] = useState(null);
    const [ssoCode, setSsoCode] = useState(null);
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

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
        signIn(username, password, null);
    }

    const handleKeypress = e => {
        if (e.key === 'Enter')
            handleSignIn();
    };

    // After an SSO login the server redirects to /login/sso?code=... or to /login?sso_error=...
    useEffect(() => {
        fetch(getBaseUrl('public/sso/config'))
            .then(handleLoginErrors)
            .then(config => setSsoConfig(config))
            .catch(() => setSsoConfig(null));

        const code = searchParams.get('code');
        const ssoError = searchParams.get('sso_error');
        if (code != null || ssoError != null)
            navigate({search: ''}, {replace: true}); // keep the code out of the address bar and history
        if (code != null) {
            setSsoCode(code);
            ssoSignIn(code, null);
        } else if (ssoError != null) {
            setMessageBox({open: true, severity: 'error', title: 'Login failed', message: SSO_ERRORS[ssoError] || SSO_ERRORS.idp_error});
        }
    }, []);

    const signIn = (username, password, projectId) => {
        authenticate('public/signin', projectId == null ? {username, password} : {username, password, projectId});
    };

    const ssoSignIn = (code, projectId) => {
        authenticate('public/sso/token', projectId == null ? {code} : {code, projectId}, true);
    };

    const authenticate = (endpoint, body, sso = false) => {
        fetch(getBaseUrl(endpoint), {
            method: 'POST',
            body: JSON.stringify(body),
            headers: {'Content-Type': 'application/json'}
        })
            .then(handleLoginErrors)
            .then(data => {
                logOut();
                if (sso)
                    setSsoLogin();
                setUsername(data.username || body.username);
                if (data.changePassword != null && data.changePassword === true) {
                    setToken(data.token);
                    setState(prevState => ({...prevState, changePassword: true}));
                } else if (data.projects != null) {
                    setRoles(data.roles);

                    if (hasSuperRole())
                        setToken(data.token);

                    // sorted by name on the server
                    setState(prevState => ({...prevState, projects: data.projects, changePassword: false}));
                } else if (data.token != null) {
                    setRoles(data.roles);
                    setToken(data.token);
                    if (hasMemberRole() || hasAdminRole()) {
                        setActiveProjectId(data.project.id);
                        ReactGA.initialize('UA-86983600-1');
                        ReactGA.pageview("login-to-project");
                        setState(prevState => ({...prevState, projects: null, changePassword: false}));
                    } else if (hasSuperRole()) {
                        setState(prevState => ({...prevState, projects: null, changePassword: false}));
                    } else
                        setMessageBox({open: true, severity: 'error', title: 'Login failed', message: 'This user does not have required role to see the project frontend!'});
                }
            }).catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Login failed', message: error});
            }
        );
    };


    const selectProject = (id) => {
        if (ssoCode != null)
            ssoSignIn(ssoCode, id);
        else
            signIn(username, password, id);
    }

    const getLoginPaper = () => {
        const ssoEnabled = ssoConfig != null && ssoConfig.enabled === true;
        return (
            <Paper className="PaperClass" elevation={3}>
                <Typography className="Title" variant="h5">
                    Login
                </Typography>
                <FormControl className="LoginInputText" fullWidth>
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
                <FormControl className="LoginInputText" fullWidth>
                    <InputLabel htmlFor="standard-adornment-password">Password</InputLabel>
                    <Input
                        id="standard-adornment-password"
                        name="password"
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
                                >
                                    {showPassword ? <Visibility/> : <VisibilityOff/>}
                                </IconButton>
                            </InputAdornment>
                        }
                    />
                </FormControl>
                <div className="ForgotPasswordLink">
                    <FancyLink to="/forgot-password">Forgot your password?</FancyLink>
                </div>
                <div className="ButtonLogin PasswordLogin">
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
                {ssoEnabled && (
                    <div className="ButtonLogin SsoLogin">
                        <Divider sx={{mb: 2}}>
                            <Typography variant="caption" color="textSecondary">or</Typography>
                        </Divider>
                        <SsoButton provider={ssoConfig.provider} name={ssoConfig.name}/>
                    </div>
                )}
                <div className={ssoEnabled ? "RegistrationLink AfterSso" : "RegistrationLink"} style={{marginBottom: "10px"}}>
                    <FancyLink to="/sign-up">Don't have an account?</FancyLink>
                </div>
            </Paper>
        );
    }

    const getProjectsPaper = () => {
        return (
            <Paper className="PaperClass" elevation={3}>

                <Grid className="TitleGrid" container direction="row" justify="center" alignItems="center" spacing={1}>
                    <Grid item xs={3}><div /></Grid>
                    <Grid item xs={6}>
                        <Typography variant="h5">
                            Select project
                        </Typography>
                    </Grid>
                    {hasSuperRole() ? (
                        <Grid item xs={2}>
                            <Tooltip title="Manage accounts">
                                <IconButton component={Link} to={getManagementProjectsPath()}>
                                    <span className="material-icons">manage_accounts</span>
                                </IconButton>
                            </Tooltip>
                        </Grid>
                    ) : (
                        <div/>
                    )}
                </Grid>

                <List component="nav" className="ProjectsList">
                    {projects.map(({id, name, description, enabled, expired}) => {
                        return (
                            <div key={id}>
                                <ListItem button onClick={() => selectProject(id)}>
                                    <ListItemAvatar>
                                        <Avatar>
                                            {enabled !== true ? <DisabledProjectIcon/> : expired === true ?
                                                <ExpiredProjectIcon/> : <ActiveProjectIcon color='secondary'/>}
                                        </Avatar>
                                    </ListItemAvatar>
                                    <ListItemText primary={name} secondary={description}/>
                                </ListItem>
                            </div>
                        );
                    })}
                </List>
            </Paper>
        );
    }

    const redirectTo = (pagePath) => {
        return (<Navigate to={pagePath} replace />);
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
                return getProjectsPaper();
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
            <Typography variant="caption">{version}</Typography>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}