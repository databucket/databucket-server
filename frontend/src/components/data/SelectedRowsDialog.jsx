import React, {useEffect, useState} from 'react';
import {Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, TextField, Typography} from '@mui/material';

// Confirms an action on the selected rows of the data table: deleting them, or setting a tag (chosen here)
export default function SelectedRowsDialog({action, tags, onClose, onConfirm}) {

    const [tagId, setTagId] = useState('');

    useEffect(() => setTagId(''), [action]);

    if (action == null)
        return null;
    const count = action.rows.length;
    const rows = `${count} ${count === 1 ? 'row' : 'rows'}`;
    const isDelete = action.type === 'delete';

    return (
        <Dialog open onClose={onClose} maxWidth="xs" fullWidth>
            <DialogTitle>{isDelete ? `Delete ${rows}?` : `Set the tag of ${rows}`}</DialogTitle>
            <DialogContent>
                {isDelete
                    ? <Typography>The selected rows and their history are deleted. This can't be undone.</Typography>
                    : <TextField select fullWidth variant="standard" label="Tag" value={tagId} autoFocus
                                 onChange={e => setTagId(e.target.value)} sx={{mt: 1}}>
                        {[...tags].sort((a, b) => a.name.localeCompare(b.name)).map(tag =>
                            <MenuItem key={tag.id} value={tag.id}>{tag.name}</MenuItem>)}
                    </TextField>
                }
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Cancel</Button>
                <Button variant="contained" color={isDelete ? 'error' : 'primary'} disabled={!isDelete && tagId === ''}
                        onClick={() => onConfirm(isDelete ? null : tagId)}>
                    {isDelete ? 'Delete' : 'Set tag'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
