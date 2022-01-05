import React, {useState} from "react";
import "./LoginPage.css";
import Button from "@material-ui/core/Button";
import Logo from "../../images/databucket-logo.png";
import {
    Avatar, Input, InputAdornment, InputLabel,
    ListItem,
    ListItemAvatar,
    ListItemText,
    Paper, Tooltip
} from "@material-ui/core";
import ActiveProjectIcon from "@material-ui/icons/FolderSpecial";
import DisabledProjectIcon from "@material-ui/icons/NotInterested";
import ExpiredProjectIcon from "@material-ui/icons/EventBusy";
import Typography from "@material-ui/core/Typography";
import List from "@material-ui/core/List";
import {handleLoginErrors} from "../../utils/FetchHelper";
import {
    setToken,
    setActiveProjectId,
    setRoles, setUsername, hasSuperRole, hasMemberRole, hasAdminRole, hasToken, hasProject, logOut
} from '../../utils/ConfigurationStorage';
import {Link, Redirect} from 'react-router-dom';
import FormControl from "@material-ui/core/FormControl";
import IconButton from "@material-ui/core/IconButton";
import {Visibility, VisibilityOff} from "@material-ui/icons";
import Grid from "@material-ui/core/Grid";
import {MessageBox} from "../utils/MessageBox";
import {sortByKey} from "../../utils/JsonHelper";
import {getManagementProjectsPath, getProjectDataPath} from "../../route/AppRouter";
import {getBaseUrl} from "../../utils/UrlBuilder";
import ReactGA from 'react-ga';

const initialState = {
    username: "",
    password: "",
    projects: null,
    changePassword: false,
    showPassword: false
};

export default function LoginPage() {

    const [{username, password, projects, changePassword, showPassword}, setState] = useState(initialState);
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
        signIn(username, password, null);
    }

    const handleKeypress = e => {
        if (e.key === 'Enter')
            handleSignIn();
    };

    const signIn = (username, password, projectId) => {
        fetch(getBaseUrl('public/signin'), {
            method: 'POST',
            body: JSON.stringify(projectId == null ? {username, password} : {username, password, projectId}),
            headers: {'Content-Type': 'application/json'}
        })
            .then(handleLoginErrors)
            .then(data => {
                logOut();
                setUsername(username);
                if (data.changePassword != null && data.changePassword === true) {
                    setToken(data.token);
                    setState(prevState => ({...prevState, changePassword: true}));
                } else if (data.projects != null) {
                    setRoles(data.roles);

                    if (hasSuperRole())
                        setToken(data.token);

                    let sortedProjects = sortByKey(data.projects, 'id');
                    setState(prevState => ({...prevState, projects: sortedProjects, changePassword: false}));
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
        signIn(username, password, id);
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
                <div className="Button">
                    <Button
                        variant="contained"
                        color="primary"
                        size={'large'}
                        disabled={!(username.length > 0 && password.length > 0)}
                        onClick={() => {
                            handleSignIn();
                        }}
                    >
                        Submit
                    </Button>
                </div>
            </Paper>
        );
    }

    const getProjectsPaper = () => {
        return (
            <Paper className="PaperClass" elevation={3}>
                <Grid className="TitleGrid" container direction="row" justify="center" alignItems="center" spacing={1}>
                    <Grid item>
                        <Typography variant="h5">
                            Select project
                        </Typography>
                    </Grid>
                    {hasSuperRole() ? (
                        <Grid item>
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
        return (<Redirect to={pagePath}/>);
    }

    const getSwitchParam = () => {
        if (changePassword === true) {
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
            case 4:
                return redirectTo("/change-password");
            case 3:
                return getProjectsPaper();
            case 2:
                return redirectTo(getProjectDataPath());
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
            <Typography variant="caption">3.1.5</Typography>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}