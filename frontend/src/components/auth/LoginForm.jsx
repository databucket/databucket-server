import React, {useState} from "react";
import "./LoginForm.css";
import {
    Box,
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
import {getBaseUrl} from "../../utils/UrlBuilder";

export default function LoginFormComponent() {

    const [showPassword, setShowPassword] = useState(false);
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");

    const handleMouseDownPassword = (event) => {
        event.preventDefault();
    };

    const handleLogin = (e) => {
        e.preventDefault();

        const formData = new FormData(e.target);
        fetch(getBaseUrl('public/sign-in'), {
            method: "POST",
            headers: {
                Accept: "application/json",
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                username: formData.get('username'),
                password: formData.get('password'),
            }),
        })
            .then((response) => response.json())
            .then((data) => {
                if (data.fieldErrors) {
                    data.fieldErrors.forEach(fieldError => {
                        if (fieldError.field === 'username') {
                            // setEmailError(fieldError.message);
                        }

                        if (fieldError.field === 'password') {
                            // setPasswordError(fieldError.message);
                        }
                    });
                } else {
                    alert("Success !!");
                }
            })
            .catch((err) => err);
    };

    const handleClickShowPassword = () => {
        setShowPassword(!showPassword);
    }

    return (
        <Box className="Container">
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
                    onClick={() => {
                        handleLogin();
                    }}
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
        </Box>
    );
};
