import React, {useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {
    Autocomplete,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Typography
} from '@mui/material';
import {getBaseUrl} from "../../utils/UrlBuilder";
import {getPostOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {StatusIcon} from "./ScheduleRunsDialog";

RunKeywordDialog.propTypes = {
    open: PropTypes.bool.isRequired,
    keywords: PropTypes.array.isRequired,
    onClose: PropTypes.func.isRequired,
    onRun: PropTypes.func
};

/**
 * Runs the jobs of a keyword (Settings > Jobs > Run keyword): Preview counts the rows each job would run on, Run runs
 * them one after another by id, as the signed-in user.
 */
export default function RunKeywordDialog(props) {
    const {open, keywords} = props;
    const [keyword, setKeyword] = useState('');
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);
    const [busy, setBusy] = useState(false);

    // only when opened: the jobs are fetched again after a run (new keywords), which must not clear the result
    useEffect(() => {
        if (open) {
            setResult(null);
            setError(null);
            setKeyword(current => current !== '' ? current : (keywords.length === 1 ? keywords[0] : ''));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open]);

    const call = preview => {
        setBusy(true);
        setError(null);
        fetch(getBaseUrl(`task-schedules/run${preview ? '?preview=true' : ''}`), getPostOptions({keyword}))
            .then(handleErrors)
            .then(response => {
                setResult(response);
                if (!preview && props.onRun)
                    props.onRun();
            })
            .catch(e => {
                setResult(null);
                setError(String(e));
            })
            .finally(() => setBusy(false));
    };

    const blank = keyword.trim() === '';

    return (
        <Dialog open={open} onClose={props.onClose} fullWidth maxWidth='lg'>
            <DialogTitle>Run the jobs of a keyword</DialogTitle>
            <DialogContent dividers sx={{height: '50vh', display: 'flex', flexDirection: 'column', gap: 2}}>
                <Autocomplete
                    freeSolo
                    options={keywords}
                    inputValue={keyword}
                    onInputChange={(event, value) => {
                        setKeyword(value || '');
                        setResult(null);
                    }}
                    renderInput={params => <TextField {...params} label='Keyword' variant='standard' autoFocus/>}
                />
                <Typography variant='body2' color='text.secondary'>
                    The jobs run one after another (by id) as you, also the disabled ones. A failed job doesn't stop
                    the others. Preview first: it only counts the rows.
                </Typography>
                {error != null && <Typography color='error'>{error}</Typography>}
                {result != null &&
                    <TableContainer sx={{flex: 1}}>
                        <Typography variant='subtitle2' sx={{mb: 1}}>
                            {result.preview ? 'Preview' : 'Done'}: {result.ok} OK, {result.errors} failed
                        </Typography>
                        <Table size='small' stickyHeader>
                            <TableHead>
                                <TableRow>
                                    <TableCell>Job</TableCell>
                                    <TableCell>Task</TableCell>
                                    <TableCell>Bucket</TableCell>
                                    <TableCell>Status</TableCell>
                                    <TableCell align='right'>Rows</TableCell>
                                    <TableCell>Message</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {result.jobs.map(job => (
                                    <TableRow key={job.id} hover>
                                        <TableCell>{job.name}</TableCell>
                                        <TableCell>{job.task}</TableCell>
                                        <TableCell>{job.bucket}</TableCell>
                                        <TableCell>
                                            <Box sx={{display: 'flex', alignItems: 'center', gap: 0.5}}>
                                                <StatusIcon status={job.status}/>{job.status}
                                            </Box>
                                        </TableCell>
                                        <TableCell align='right'>{job.count != null ? job.count : ''}</TableCell>
                                        <TableCell>{job.message}</TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>}
            </DialogContent>
            <DialogActions>
                <Button onClick={props.onClose} color='inherit'>Close</Button>
                <Button onClick={() => call(true)} disabled={blank || busy}>Preview</Button>
                <Button onClick={() => call(false)} disabled={blank || busy} variant='contained'>Run</Button>
            </DialogActions>
        </Dialog>
    );
}
