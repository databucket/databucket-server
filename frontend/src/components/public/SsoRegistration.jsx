import React, {useEffect, useState} from "react";
import {Box, Button, FormControl, IconButton, Input, InputLabel, Link, Paper, Tooltip, Typography} from "@mui/material";
import InfoIcon from "@mui/icons-material/InfoOutlined";
import {handleLoginErrors} from "../../utils/FetchHelper";
import {getBaseUrl} from "../../utils/UrlBuilder";
import ProjectRequest from "./ProjectRequest";

const post = (endpoint, body) => fetch(getBaseUrl(endpoint), {
    method: 'POST',
    body: JSON.stringify(body),
    headers: {'Content-Type': 'application/json'}
}).then(handleLoginErrors);

/**
 * sso.self-registration: after an SSO login without a Databucket account the server redirects to
 * /login/sso?register=<ticket>; here the person creates the account and picks public projects to ask for.
 */
export default function SsoRegistration({ticket, onClose, showError}) {

    const [info, setInfo] = useState(null);
    const [username, setUsername] = useState("");
    const [projectsIds, setProjectsIds] = useState([]);
    const [message, setMessage] = useState("");
    const [loading, setLoading] = useState(false);
    const [created, setCreated] = useState(null);

    useEffect(() => {
        post('public/sso/registration', {ticket})
            .then(data => {
                setInfo(data);
                setUsername(data.username);
            })
            .catch(error => {
                showError('Creating an account failed', error);
                onClose();
            });
    }, [ticket]);

    const register = () => {
        setLoading(true);
        post('public/sso/register', {ticket, username, projectsIds, message})
            .then(data => setCreated(data.username))
            .catch(error => showError('Creating an account failed', error))
            .finally(() => setLoading(false));
    };

    if (created != null)
        return (
            <Paper className="PaperClass" elevation={3}>
                <Typography className="Title" variant="h5">Account created</Typography>
                <Box sx={{width: '85%'}}>
                    <Typography variant="body2" paragraph>
                        Your account <b>{created}</b> has been created.
                    </Typography>
                    <Typography variant="body2" paragraph>
                        The administrators have been told about it, together with the projects you asked for.
                        Until you are given access to a project, there is nothing to open.
                    </Typography>
                    <Typography variant="body2">
                        Try to sign in again later.
                    </Typography>
                </Box>
            <Box sx={{width: '100%', mt: 6, mb: 2, pl: 5, display: 'flex', justifyContent: 'flex-start'}}>
                <Link component="button" color="inherit" onClick={onClose}>Back</Link>
            </Box>
            </Paper>
        );

    if (info == null)
        return (
            <Paper className="PaperClass" elevation={3}>
                <Typography className="Title" variant="h5">Create an account</Typography>
                <Typography variant="body2" sx={{mb: 3}}>Loading...</Typography>
            </Paper>
        );

    const nameValid = username.trim().length >= 1 && username.trim().length <= 50;
    return (
        <Paper className="PaperClass" elevation={3}>
            <Box className="Title" sx={{display: 'flex', alignItems: 'center'}}>
                <Typography variant="h5">Create an account</Typography>
                <Tooltip placement="bottom" title={
                    <Typography variant="body2" sx={{p: 0.5}}>
                        Your identity provider gave the email <b>{info.email}</b>. There is no Databucket account for
                        it yet: create one and ask the administrators for access to projects.
                    </Typography>
                }>
                    <IconButton size="small" sx={{ml: 1}} aria-label="About the account">
                        <InfoIcon fontSize="small"/>
                    </IconButton>
                </Tooltip>
            </Box>
            <FormControl sx={{width: '85%'}}>
                <InputLabel htmlFor="sso-registration-username">Username</InputLabel>
                <Input
                    id="sso-registration-username"
                    value={username}
                    onChange={e => setUsername(e.target.value.slice(0, 50))}
                    error={!nameValid}
                />
            </FormControl>
            <ProjectRequest projects={info.projects} selected={projectsIds} onChange={setProjectsIds}
                            message={message} onMessageChange={setMessage}/>
            <Box sx={{width: '85%', mt: 2}}>
                <Button fullWidth variant="contained" color="primary" disabled={!nameValid || loading} onClick={register}>
                    {loading ? 'Processing...' : 'Create account'}
                </Button>
            </Box>
            <Box sx={{width: '100%', mt: 6, mb: 2, pl: 5, display: 'flex', justifyContent: 'flex-start'}}>
                <Link component="button" color="inherit" onClick={onClose}>Back</Link>
            </Box>
        </Paper>
    );
}
