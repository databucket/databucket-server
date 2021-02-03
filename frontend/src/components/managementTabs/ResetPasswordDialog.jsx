import React, {useState} from 'react';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogTitle from '@material-ui/core/DialogTitle';
import {getBaseUrl, getPostOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {MessageBox} from "../MessageBox";
import Typography from "@material-ui/core/Typography";
import IconButton from "@material-ui/core/IconButton";
import CopyIcon from "@material-ui/icons/FileCopy";
import FormControl from "@material-ui/core/FormControl";
import {InputAdornment, OutlinedInput} from "@material-ui/core";

export default function ResetPasswordDialog(props) {
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [password, setPassword] = React.useState('');
    const [resetEnabled, setResetEnabled] = React.useState(false);
    const [confirmedUserName, setConfirmedUserName] = React.useState('');

    const resetPassword = () => {
        let newPassword = Math.random().toString(36).slice(2);
        let payload = {'username': props.username, 'password': newPassword};

        fetch(getBaseUrl('users/password/reset'), getPostOptions(payload))
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
            })
            .then(() => {
                setPassword(newPassword);
                setMessageBox({open: true, severity: 'success', title: 'Success', message: 'The user password has been reset.'});
            });
    }

    const copyToClipBoard = async text => {
        try {
            await navigator.clipboard.writeText(text);
        } catch (err) {
            setMessageBox({open: true, severity: 'error', title: 'Error', message: 'Copying has failed!'});
        }
    };

    const onConfirmName = e => {
        setConfirmedUserName(e.target.value)
        setResetEnabled(props.username === e.target.value);
    }

    const handleClose = () => {
        props.onClose();
        setPassword('');
        setConfirmedUserName('');
    }

    return (
        <div>
            <Dialog
                open={props.open}
                onClose={handleClose}
                aria-labelledby="alert-dialog-title"
                aria-describedby="alert-dialog-description"
            >
                <DialogTitle id="alert-dialog-title">{'Reset password'}</DialogTitle>
                <DialogContent>
                    <Typography style={{
                        display: 'inline-block',
                        marginRight: '5px'
                    }}>{'Reset password for the user'}</Typography>
                    <Typography style={{display: 'inline-block'}} color={'secondary'}>{`${props.username}`}</Typography>
                    <Typography>{'The new password will be randomly generated.'}</Typography>

                    <Typography style={{marginTop: '20px', marginBottom: '4px'}}>{'Please type the user name to confirm.'}</Typography>
                    <FormControl variant="outlined" fullWidth={true}>
                        <OutlinedInput
                            value={confirmedUserName}
                            onChange={onConfirmName}
                            inputProps={{
                                'aria-label': 'weight',
                            }}
                            labelWidth={0}
                        />
                    </FormControl>

                    <Typography style={{marginTop: '15px', marginBottom: '4px'}}>{'Copy and share the new password securely.'}</Typography>
                    <FormControl variant="outlined" fullWidth={true}>
                        <OutlinedInput
                            value={password}
                            readOnly={true}
                            disabled={true}
                            endAdornment={<InputAdornment position="end">
                                <IconButton
                                    aria-label="copy to clipboard"
                                    role={"alert"}
                                    onClick={() => copyToClipBoard(password)}
                                    edge="end"
                                >
                                    {<CopyIcon />}
                                </IconButton>
                            </InputAdornment>}
                            inputProps={{
                                'aria-label': 'weight',
                            }}
                            labelWidth={0}
                        />
                    </FormControl>
                </DialogContent>
                <DialogActions>
                    <Button variant="contained" color="secondary" onClick={resetPassword} disabled={!resetEnabled}>
                        Reset password
                    </Button>
                    <Button onClick={handleClose} color="primary" autoFocus>
                        Close
                    </Button>
                </DialogActions>
            </Dialog>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}