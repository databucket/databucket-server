import React from "react";
import {IconButton, InputAdornment} from "@mui/material";
import {Visibility, VisibilityOff} from "@mui/icons-material";

// The eye at the end of a password field, as on the sign-in page: shows or hides the password of that field
export default function PasswordVisibility({shown, onToggle}) {
    return (
        <InputAdornment position="end">
            <IconButton
                aria-label="toggle password visibility"
                onClick={onToggle}
                // keeps the focus in the field
                onMouseDown={event => event.preventDefault()}
            >
                {shown ? <Visibility/> : <VisibilityOff/>}
            </IconButton>
        </InputAdornment>
    );
}
