import React, {useState} from "react";
import "./LoginForm.css";
import {
    Button,
    FormControl,
    IconButton,
    Input,
    InputAdornment,
    InputLabel,
    Link,
    TextField,
    Typography
} from "@material-ui/core";
import {Visibility, VisibilityOff} from "@material-ui/icons";
import {getActiveProjectId} from "../../utils/ConfigurationStorage";
import {handleSuccessfulLogin} from "../utils/AuthHelper";
import {handleLoginErrors} from "../../utils/FetchHelper";
import {useHistory} from "react-router-dom";

export default function LoginFormComponent() {

    const [showPassword, setShowPassword] = useState(false);
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
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
            .then((data) => handleSuccessfulLogin(data, {}))
            .then(value => {
                if (!value.projectId){
                    history.replace("/select-project", {projects: value.projects})
                } else {
                    history.push(`/project/${value.projectId}`)
                }
            })
            .catch((err) => console.error(err));
    };

    const handleClickShowPassword = () => {
        setShowPassword(!showPassword);
    }

    return (
        <form className="Container" onSubmit={handleLogin}>
            <Typography className="Title" variant="h5">
                Login
            </Typography>
            <TextField className="LoginInputText"
                       id="standard-adornment-username"
                       name="username"
                       type='text'
                       label="Username"
                       value={username}
                       onChange={(evt) => setUsername(evt.target.value)}
            >
            </TextField>
            <FormControl className="LoginInputText">
                <InputLabel htmlFor="standard-adornment-password">Password</InputLabel>
                <Input
                    id="standard-adornment-password"
                    name="password"
                    autocomplete="current-password"
                    // inputProps={{ style: { backgroundColor: "red" } }} //TODO nie działa tylko dla Chrome
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
                                {showPassword ? <Visibility/> : <VisibilityOff/>}
                            </IconButton>
                        </InputAdornment>
                    }
                />
            </FormControl>
            <input hidden name="projectid" type="text" value={getActiveProjectId()}/>
            <Link
                component="button"
                variant="caption"
                color="inherit"
                onClick={() => {
                    // setState(prevState => ({...prevState, resetPassword: true}));
                }}
            >
                Forgot your password?
            </Link>
            <div className="ButtonLogin">
                <Button
                    fullWidth={true}
                    variant="contained"
                    color="primary"
                    size={'large'}
                    disabled={!(username.length > 0 && password.length > 0)}
                    type="submit"
                >
                    Login
                </Button>
            </div>
            <Link
                component="button"
                color="inherit"
                onClick={() => {
                    // setState(prevState => ({...prevState, register: true}));
                }}
            >
                Don't have an account?
            </Link>
        </form>
    );
};
