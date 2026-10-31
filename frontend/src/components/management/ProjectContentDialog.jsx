import React, {useEffect, useState} from 'react';
import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    LinearProgress,
    Table,
    TableBody,
    TableCell,
    TableRow,
    TextField,
    Typography
} from '@mui/material';
import {getBaseUrl} from "../../utils/UrlBuilder";
import {getDeleteOptions, getGetOptions, getPostOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";

// Clearing a project (mode 'clear': everything but the project and its users), showing what will be removed, or
// deleting an empty one (mode 'delete'). Both ask for the project's name, as there is no way back.
export default function ProjectContentDialog({project, mode, onClose, onDone}) {

    const [content, setContent] = useState(null);
    const [confirmedName, setConfirmedName] = useState('');
    const [working, setWorking] = useState(false);
    const [error, setError] = useState(null);
    const open = project != null;
    const deleting = mode === 'delete';

    useEffect(() => {
        setContent(null);
        setConfirmedName('');
        setWorking(false);
        setError(null);
        if (project != null && mode === 'clear')
            fetch(getBaseUrl(`manage/projects/${project.id}/content`), getGetOptions())
                .then(handleErrors)
                .then(setContent)
                .catch(e => setError(typeof e === 'string' ? e : e.message));
    }, [project, mode]);

    const execute = () => {
        setWorking(true);
        setError(null);
        const request = deleting
            ? fetch(getBaseUrl(`manage/projects/${project.id}`), getDeleteOptions())
            : fetch(getBaseUrl(`manage/projects/${project.id}/clear`), getPostOptions());
        request
            .then(handleErrors)
            .then(() => onDone(project))
            .catch(e => {
                setWorking(false);
                setError(typeof e === 'string' ? e : e.message);
            });
    }

    const empty = content != null && Object.values(content).every(count => count === 0);

    return (
        <Dialog open={open} onClose={() => !working && onClose()} maxWidth="xs" fullWidth>
            <DialogTitle>{deleting ? 'Delete project' : 'Clear project'}</DialogTitle>
            <DialogContent>
                <Typography gutterBottom>
                    {deleting ? 'Delete the empty project ' : 'Remove the content of the project '}
                    <Typography component="span" color="secondary">{project?.name}</Typography>
                    {deleting ? '.' : ':'}
                </Typography>

                {!deleting && content == null && error == null && <LinearProgress sx={{my: 2}}/>}
                {content != null &&
                    <Table size="small" sx={{mb: 2}}>
                        <TableBody>
                            {Object.entries(content).map(([kind, count]) =>
                                <TableRow key={kind}>
                                    <TableCell sx={{py: 0.25}}>{kind}</TableCell>
                                    <TableCell sx={{py: 0.25}} align="right">{count.toLocaleString()}</TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                }
                {!deleting && empty && <Alert severity="info" sx={{mb: 2}}>The project is empty.</Alert>}

                <Typography variant="body2" color="error">
                    {deleting
                        ? 'The users stay, but they lose access to the project.'
                        : 'The project and its users stay; teams and the users\' access to buckets and views are removed too.'}
                </Typography>
                <Typography variant="body2" color="error" gutterBottom>This can't be undone.</Typography>

                <TextField
                    label="Type the project name to confirm"
                    value={confirmedName}
                    onChange={e => setConfirmedName(e.target.value)}
                    disabled={working}
                    fullWidth
                    size="small"
                    sx={{mt: 2}}
                />
                {working && <LinearProgress sx={{mt: 2}}/>}
                {error != null && <Alert severity="error" sx={{mt: 2}}>{error}</Alert>}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={working}>Cancel</Button>
                <Button
                    variant="contained"
                    color="error"
                    onClick={execute}
                    disabled={working || confirmedName !== project?.name || (!deleting && (content == null || empty))}
                >
                    {deleting ? 'Delete' : 'Clear'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
