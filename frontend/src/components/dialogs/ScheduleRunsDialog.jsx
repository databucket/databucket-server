import React, {useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {
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
    Typography
} from '@mui/material';
import {CheckCircleOutline, ErrorOutline} from '@mui/icons-material';
import {getBaseUrl} from "../../utils/UrlBuilder";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";

ScheduleRunsDialog.propTypes = {
    schedule: PropTypes.object,
    onClose: PropTypes.func.isRequired
};

export const StatusIcon = ({status}) => status === 'OK'
    ? <CheckCircleOutline fontSize='small' color='success'/>
    : status === 'ERROR' ? <ErrorOutline fontSize='small' color='error'/> : null;

const formatDuration = ms => ms < 1000 ? `${ms} ms` : `${(ms / 1000).toFixed(1)} s`;

const TRIGGERS = {SCHEDULE: 'Cron', MANUAL: 'Run', KEYWORD: 'Keyword'};

/** The latest runs of a job, the newest first (Settings > Jobs > Executions). */
export default function ScheduleRunsDialog(props) {
    const {schedule} = props;
    const [runs, setRuns] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (schedule == null)
            return;
        setRuns(null);
        setError(null);
        fetch(getBaseUrl(`task-schedules/${schedule.id}/runs`), getGetOptions())
            .then(handleErrors)
            .then(setRuns)
            .catch(e => setError(String(e)));
    }, [schedule]);

    return (
        <Dialog open={schedule != null} onClose={props.onClose} fullWidth maxWidth='lg'>
            <DialogTitle>{`Executions of "${schedule != null ? schedule.name : ''}"`}</DialogTitle>
            <DialogContent dividers sx={{height: '50vh', p: 0}}>
                {error != null && <Typography color='error' sx={{p: 2}}>{error}</Typography>}
                {runs != null && runs.length === 0 &&
                    <Typography color='text.secondary' sx={{p: 2}}>The job hasn't run yet.</Typography>}
                {runs != null && runs.length > 0 &&
                    <TableContainer sx={{maxHeight: '100%'}}>
                        <Table size="small" stickyHeader>
                            <TableHead>
                                <TableRow>
                                    <TableCell>Started</TableCell>
                                    <TableCell>Trigger</TableCell>
                                    <TableCell>Run by</TableCell>
                                    <TableCell>Status</TableCell>
                                    <TableCell align='right'>Rows</TableCell>
                                    <TableCell align='right'>Duration</TableCell>
                                    <TableCell>Message</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {runs.map(run => (
                                    <TableRow key={run.id} hover>
                                        <TableCell sx={{whiteSpace: 'nowrap'}}>{new Date(run.startedAt).toLocaleString()}</TableCell>
                                        <TableCell>{TRIGGERS[run.trigger] || run.trigger}</TableCell>
                                        <TableCell>{run.runAs}</TableCell>
                                        <TableCell>
                                            <Box sx={{display: 'flex', alignItems: 'center', gap: 0.5}}>
                                                <StatusIcon status={run.status}/>{run.status}
                                            </Box>
                                        </TableCell>
                                        <TableCell align='right'>{run.count != null ? run.count : ''}</TableCell>
                                        <TableCell align='right' sx={{whiteSpace: 'nowrap'}}>{formatDuration(run.durationMs)}</TableCell>
                                        <TableCell>{run.message}</TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>}
            </DialogContent>
            <DialogActions>
                <Button onClick={props.onClose} color='inherit'>Close</Button>
            </DialogActions>
        </Dialog>
    );
}
