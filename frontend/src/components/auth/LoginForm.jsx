import React, {forwardRef, useState} from "react";
import "./LoginForm.css";
import {
    Button,
    FormControl,
    IconButton,
    Input,
    InputAdornment,
    InputLabel,
    Link as MaterialLink,
    Stack,
    TextField,
    Typography
} from "@mui/material";
import {Visibility, VisibilityOff} from "@mui/icons-material";
import {getActiveProjectId} from "../../utils/ConfigurationStorage";
import {handleSuccessfulLogin} from "../utils/AuthHelper";
import {handleLoginErrors} from "../../utils/FetchHelper";
import {Link, useHistory} from "react-router-dom";
import {MessageBox} from "../utils/MessageBox";

const FancyLink = forwardRef(({navigate, ...props}, ref) => {
    return (
        <MaterialLink
            ref={ref}
            color="inherit"
            variant="caption"
            underline="hover"
            {...props}
            mb={2}
        >{props.children}</MaterialLink>
    )
});
export default function LoginFormComponent() {

    const [showPassword, setShowPassword] = useState(false);
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [messageBox, setMessageBox] = useState(
        {open: false, severity: 'error', title: '', message: ''});
    const history = useHistory()

    const handleMouseDownPassword = (event) => {
        event.preventDefault();
    };

    const handleLogin = (e) => {
        e.preventDefault();

        const formData = new FormData(e.target);
        fetch("/login-form", {
            method: "POST",
            body: formData,
        })
        .then(handleLoginErrors)
        .then((data) => {
            if (data.changePassword) {
                history.push("/change-password")
                return null;
            }
            return data;
        })
        .then((data) => handleSuccessfulLogin(data, {}))
        .then(value => {
            if (!value.projectId) {
                history.replace("/select-project", {projects: value.projects})
            } else {
                history.push(`/project/${value.projectId}`)
            }
        })
        .catch((err) => setMessageBox({
            open: true,
            severity: 'error',
            title: 'Login failed',
            message: err.message
        }));
    };

    const handleClickShowPassword = () => {
        setShowPassword(!showPassword);
    }

    return (
        <>
            <Stack direction="column"
                   spacing={2}
                   alignItems="center"
                   component="form"
                   noValidate
                   onSubmit={handleLogin}
                   p={3}
            >
                <Typography variant="h5" p={3}>
                    Login
                </Typography>
                <TextField
                    fullWidth
                    variant="standard"
                    id="standard-adornment-username"
                    name="username"
                    type='text'
                    label="Username"
                    value={username}
                    onChange={(evt) => setUsername(evt.target.value)}
                >
                </TextField>
                <FormControl variant="standard" fullWidth>
                    <InputLabel
                        htmlFor="standard-adornment-password">Password</InputLabel>
                    <Input
                        id="standard-adornment-password"
                        name="password"
                        autoComplete="current-password"
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
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
                <input hidden name="projectid" type="text" readOnly
                       value={getActiveProjectId()}/>
                <Link to="/forgot-password" component={FancyLink}>Forgot
                    your
                    password?</Link>
                <Button
                    fullWidth
                    variant="contained"
                    color="primary"
                    size={'large'}
                    disabled={!(username.length > 0 && password.length > 0)}
                    type="submit"
                >
                    Login
                </Button>
                <Link to="/sign-up"
                      component={FancyLink}
                      mb={2}
                >
                    Don't have an account?
                </Link>
            </Stack>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </>
    );
};
