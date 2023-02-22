import React, {useState} from 'react';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Typography from "@mui/material/Typography";
import FormControl from "@mui/material/FormControl";
import {OutlinedInput} from "@mui/material";
import PropTypes from "prop-types";

ConfirmRemovingDialog.propTypes = {
    open: PropTypes.bool.isRequired,
    name: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    onClose: PropTypes.func.isRequired
}

export default function ConfirmRemovingDialog(props) {
    const [removeEnabled, setRemoveEnabled] = useState(false);
    const [confirmedName, setConfirmedName] = useState('');

    const onConfirmName = e => {
        setConfirmedName(e.target.value)
        setRemoveEnabled(props.name === e.target.value);
    }

    const handleClose = () => {
        props.onClose(false);
    }

    const handleRemove = () => {
        props.onClose(true);
    }

    return (
        <div>
            <Dialog
                open={props.open}
                onClose={handleClose}
                aria-labelledby="alert-dialog-title"
                aria-describedby="alert-dialog-description"
            >
                <DialogTitle id="alert-dialog-title">{'Confirm removing'}</DialogTitle>
                <DialogContent>
                    <Typography style={{
                        display: 'inline-block',
                        marginRight: '5px'
                    }}>{props.message}</Typography>
                    <Typography style={{display: 'inline-block'}} color={'secondary'}>{`${props.name}`}</Typography>

                    <Typography color={'error'} style={{marginTop: '10px'}}>{'This action will be undone.'}</Typography>
                    <Typography color={'error'}>{'Approving this action will permanently delete this item and contained data!'}</Typography>

                    <Typography style={{marginTop: '20px', marginBottom: '4px'}}>{'Please type the item name to confirm.'}</Typography>
                    <FormControl variant="outlined" fullWidth={true}>
                        <OutlinedInput
                            value={confirmedName}
                            onChange={onConfirmName}
                            inputProps={{
                                'aria-label': 'weight',
                            }}
                            labelWidth={0}
                        />
                    </FormControl>
                </DialogContent>
                <DialogActions>
                    <Button variant="contained" color="secondary" onClick={handleRemove} disabled={!removeEnabled}>
                        Remove
                    </Button>
                    <Button onClick={handleClose} color="primary" autoFocus>
                        Close
                    </Button>
                </DialogActions>
            </Dialog>
        </div>
    );
}