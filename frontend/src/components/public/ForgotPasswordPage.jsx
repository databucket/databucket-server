import React, {useState} from "react";
import Logo from "../../images/databucket-logo.png";
import {
    Button,
    Link as RawLink,
    Paper as RawPaper,
    Stack as RawStack,
    TextField,
    Typography
} from "@mui/material";
import {MessageBox} from "../utils/MessageBox";
import {Link as RouterLink} from "react-router-dom";
import {getBaseUrl, getContextPath} from "../../utils/UrlBuilder";
import {handleLoginErrors} from "../../utils/FetchHelper";
import {validateEmail} from "../../utils/Misc";
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
  margin-top: 50px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
`;

export default function ForgotPasswordPage() {

    const [email, setEmail] = useState("");
    const [messageBox, setMessageBox] = useState(
        {open: false, severity: 'error', title: '', message: ''});

    const onChange = e => {
        setEmail(e.target.value);
    };

    const handleReset = () => {
        fetch(getBaseUrl('auth/forgot-password'), {
            method: 'POST',
            body: JSON.stringify({
                email: email,
                url: window.location.origin + getContextPath()
                    + "/confirmation/forgot-password/"
            }),
            headers: {'Content-Type': 'application/json'}
        })
        .then(handleLoginErrors)
        .then(response => {
            setMessageBox({
                open: true,
                severity: 'info',
                title: 'Send confirmation email',
                message: null
            });
        }).catch(error => {
                setMessageBox({
                    open: true,
                    severity: 'error',
                    title: 'Sending confirmation password failed',
                    message: error
                });
            }
        );
    }

    const handleKeypress = e => {
        if (e.key === 'Enter') {
            handleReset();
        }
    };

    return (
        <RawStack direction="column"
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
                       onSubmit={handleReset}
                       p={3}>
                    <Typography variant="h5" p={3}>
                        Forgot your password?
                    </Typography>
                    <Typography sx={{maxWidth: "48vh"}}>
                        Please enter the email address for your account.
                        A verification link will be sent to you.
                        Once you have received the verification link,
                        you will be able to choose a new password for your
                        account.
                    </Typography>
                    <TextField
                        fullWidth
                        variant="standard"
                        id="standard-adornment-email"
                        name="email"
                        type="email"
                        label="Email"
                        value={email}
                        onChange={onChange}
                        onKeyDown={handleKeypress}
                    />
                    <Button
                        fullWidth
                        variant="contained"
                        color="primary"
                        size={'large'}
                        disabled={!(validateEmail(email))}
                        type="submit"
                    >
                        Submit
                    </Button>
                    <Link
                        className="BackLink"
                        component={RouterLink}
                        to="/login-form"
                        color="inherit"
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
        </RawStack>
    );
}
