import React, {useCallback, useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {
    Box,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    MenuItem,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    TextField,
    Toolbar,
    Tooltip,
    Typography
} from '@mui/material';
import {FileDownload, Refresh} from '@mui/icons-material';
import {exportCsv} from "../../utils/CsvExport";
import {getBaseUrl} from "../../utils/UrlBuilder";
import {useTheme} from '@mui/material/styles';
import {
    getGetOptions,
    getSettingsTableHeight,
    getTableHeaderBackgroundColor
} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {MessageBox} from "./MessageBox";

AuditTable.propTypes = {
    // 'activity' (Settings, the signed-in project) or 'manage/audit' (Management, everything)
    endpoint: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    // Management: a column and a filter of the project
    projects: PropTypes.array,
    // the height of the whole component (toolbar + table + pagination)
    height: PropTypes.string
};

const ACTION_COLORS = {CREATE: 'success', UPDATE: 'info', DELETE: 'error'};

// an operation of the operation log (e.g. auth.signin, project.clear): its changes are details, not [before, after]
const isOperation = event => event.action.includes('.');
// details of an operation that the Name and Project columns already show
const SHOWN_ELSEWHERE = ['user', 'name', 'bucket', 'project'];
const actionColor = action => ACTION_COLORS[action]
    || (/failed|refused|denied/.test(action) ? 'error' : 'default');
const ALL = '';
const OUTSIDE_PROJECTS = '0';

// a value of a change as short text
const show = value => {
    if (value === null || value === undefined)
        return '';
    if (Array.isArray(value))
        return value.join(', '); // the names of linked objects
    if (typeof value === 'object')
        return value.name != null && Object.keys(value).length <= 2 ? `${value.name} (${value.id})` : JSON.stringify(value);
    return String(value);
};

// a date input value (local time) -> ISO instant
const toInstant = local => local ? new Date(local).toISOString() : null;

/**
 * The changes of the settings (Settings > Activity) or of everything (Management > Audit log): newest first, filtered
 * by who, the type of the object, the action and the period; a row opens its changes, property by property.
 */
export default function AuditTable(props) {
    const theme = useTheme();
    const [filter, setFilter] = useState({actor: ALL, type: ALL, action: ALL, project: ALL, from: '', to: ''});
    const [paging, setPaging] = useState({page: 0, limit: 50});
    const [result, setResult] = useState({total: 0, events: []});
    const [facets, setFacets] = useState({actors: [], types: [], actions: [], retentionDays: 0});
    const [selected, setSelected] = useState(null);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});

    // the filters as query parameters
    const filterParams = useCallback(() => {
        const params = new URLSearchParams();
        if (filter.actor) params.set('actor', filter.actor);
        if (filter.type) params.set('type', filter.type);
        if (filter.action) params.set('action', filter.action);
        if (filter.project !== ALL) params.set('project', filter.project);
        if (filter.from) params.set('from', toInstant(filter.from));
        if (filter.to) params.set('to', toInstant(filter.to));
        return params;
    }, [filter]);

    const load = useCallback(() => {
        const params = filterParams();
        params.set('page', String(paging.page + 1));
        params.set('limit', String(paging.limit));
        fetch(getBaseUrl(`${props.endpoint}?${params}`), getGetOptions())
            .then(handleErrors)
            .then(setResult)
            .catch(error => setMessageBox({open: true, severity: 'error', title: 'Error', message: error}));
    }, [props.endpoint, filterParams, paging]);

    // the events matching the filters (at most 10 000) as a CSV file
    const exportFile = () => {
        fetch(getBaseUrl(`${props.endpoint}/export?${filterParams()}`), getGetOptions())
            .then(handleErrors)
            .then(result => exportCsv(`${props.title.toLowerCase().replace(/ /g, '-')}-${new Date().toISOString().substring(0, 10)}`, [
                {title: 'When', field: 'at'}, {title: 'Project', field: 'projectName'}, {title: 'Who', field: 'actor'},
                {title: 'Role', field: 'actorRole'}, {title: 'Source', field: 'source'}, {title: 'Action', field: 'action'},
                {title: 'Object', field: 'entityType'}, {title: 'Id', field: 'entityId'}, {title: 'Name', field: 'entityName'},
                {title: 'Changes', field: 'changes'}
            ], result.events))
            .catch(error => setMessageBox({open: true, severity: 'error', title: 'Error', message: error}));
    };

    // who and which objects, for the filters: once, and again on Refresh
    const loadFacets = useCallback(() => {
        fetch(getBaseUrl(`${props.endpoint}/facets`), getGetOptions())
            .then(handleErrors)
            .then(setFacets)
            .catch(() => null);
    }, [props.endpoint]);

    useEffect(() => {
        load();
    }, [load]);

    useEffect(() => {
        loadFacets();
    }, [loadFacets]);

    const change = (name, value) => {
        setFilter({...filter, [name]: value});
        setPaging({...paging, page: 0});
    };

    const select = (name, label, options, width = 150) => (
        <TextField select size='small' variant='standard' label={label} value={filter[name]} sx={{width}}
                   onChange={e => change(name, e.target.value)}>
            <MenuItem value={ALL}><em>All</em></MenuItem>
            {options.map(option => typeof option === 'object'
                ? <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                : <MenuItem key={option} value={option}>{option}</MenuItem>)}
        </TextField>
    );

    const projectOptions = props.projects != null
        ? [{value: OUTSIDE_PROJECTS, label: '(outside projects)'},
            ...props.projects.map(p => ({value: String(p.id), label: p.name}))]
        : null;

    return (
        <Box sx={{display: 'flex', flexDirection: 'column', height: props.height || getSettingsTableHeight()}}>
            <Toolbar sx={{gap: 2, flexWrap: 'wrap', alignItems: 'flex-end', pt: 2, pb: 2.5}}>
                <Typography variant='h6' sx={{mr: 6}}>{props.title}</Typography>
                {projectOptions != null && select('project', 'Project', projectOptions, 180)}
                {select('actor', 'Who', facets.actors)}
                {select('type', 'Object', facets.types)}
                {select('action', 'Action', facets.actions || [], 170)}
                <TextField type='datetime-local' size='small' variant='standard' label='From'
                           InputLabelProps={{shrink: true}} value={filter.from}
                           onChange={e => change('from', e.target.value)}/>
                <TextField type='datetime-local' size='small' variant='standard' label='To'
                           InputLabelProps={{shrink: true}} value={filter.to}
                           onChange={e => change('to', e.target.value)}/>
                <Box sx={{flex: 1}}/>
                {facets.retentionDays > 0 &&
                    <Typography variant='caption' color='text.secondary'>
                        {`Kept for ${facets.retentionDays} days`}
                    </Typography>}
                <Tooltip title='Export the matching changes as CSV (up to 10 000)'>
                    <IconButton onClick={exportFile}><FileDownload/></IconButton>
                </Tooltip>
                <Tooltip title='Refresh'>
                    <IconButton onClick={() => { load(); loadFacets(); }}><Refresh/></IconButton>
                </Tooltip>
            </Toolbar>
            <TableContainer sx={{flex: 1}}>
                <Table size='small' stickyHeader>
                    <TableHead sx={{'& .MuiTableCell-head': {backgroundColor: getTableHeaderBackgroundColor(theme)}}}>
                        <TableRow>
                            <TableCell>When</TableCell>
                            {projectOptions != null && <TableCell>Project</TableCell>}
                            <TableCell>Who</TableCell>
                            <TableCell>Source</TableCell>
                            <TableCell>Action</TableCell>
                            <TableCell>Object</TableCell>
                            <TableCell>Name</TableCell>
                            <TableCell>Changed</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {result.events.map(event => (
                            <TableRow key={event.id} hover sx={{cursor: 'pointer'}} onClick={() => setSelected(event)}>
                                <TableCell sx={{whiteSpace: 'nowrap'}}>{new Date(event.at).toLocaleString()}</TableCell>
                                {projectOptions != null && <TableCell>{event.projectName || ''}</TableCell>}
                                <TableCell>
                                    <Tooltip title={event.actorRole || ''}><span>{event.actor}</span></Tooltip>
                                </TableCell>
                                <TableCell>{event.source}</TableCell>
                                <TableCell>
                                    <Chip size='small' variant='outlined' label={event.action}
                                          color={actionColor(event.action)}/>
                                </TableCell>
                                <TableCell>{event.entityType}</TableCell>
                                <TableCell>{event.entityName != null ? event.entityName : event.entityId ? `#${event.entityId}` : ''}</TableCell>
                                <TableCell sx={{color: 'text.secondary'}}>
                                    {event.changes == null ? '' : isOperation(event)
                                        ? Object.entries(event.changes).filter(([key]) => !SHOWN_ELSEWHERE.includes(key))
                                            .map(([key, value]) => `${key}: ${show(value)}`).join(', ')
                                        : Object.keys(event.changes).join(', ')}
                                </TableCell>
                            </TableRow>
                        ))}
                        {result.events.length === 0 &&
                            <TableRow>
                                <TableCell colSpan={8} sx={{color: 'text.secondary'}}>No changes match the filters.</TableCell>
                            </TableRow>}
                    </TableBody>
                </Table>
            </TableContainer>
            <TablePagination component='div' count={result.total} page={paging.page} rowsPerPage={paging.limit}
                             rowsPerPageOptions={[20, 50, 100, 200]}
                             onPageChange={(e, page) => setPaging({...paging, page})}
                             onRowsPerPageChange={e => setPaging({page: 0, limit: parseInt(e.target.value, 10)})}/>
            <ChangesDialog event={selected} onClose={() => setSelected(null)}/>
            <MessageBox config={messageBox} onClose={() => setMessageBox({...messageBox, open: false})}/>
        </Box>
    );
}

// the changes of one event, property by property
function ChangesDialog({event, onClose}) {
    const changes = event != null && event.changes != null ? Object.entries(event.changes) : [];
    const operation = event != null && isOperation(event);
    return (
        <Dialog open={event != null} onClose={onClose} fullWidth maxWidth='md'>
            <DialogTitle>
                {event != null ? `${event.action} ${event.entityType}${event.entityName ? ` "${event.entityName}"` : event.entityId ? ` #${event.entityId}` : ''}` : ''}
                {event != null &&
                    <Typography variant='body2' color='text.secondary'>
                        {`${new Date(event.at).toLocaleString()} · ${event.actor}${event.actorRole ? ' (' + event.actorRole + ')' : ''} · ${event.source}`}
                    </Typography>}
            </DialogTitle>
            <DialogContent dividers sx={{p: 0, minHeight: '20vh'}}>
                {operation && changes.length > 0 &&
                    <Table size='small' stickyHeader>
                        <TableHead>
                            <TableRow>
                                <TableCell>Detail</TableCell>
                                <TableCell>Value</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {changes.map(([key, value]) => (
                                <TableRow key={key}>
                                    <TableCell sx={{whiteSpace: 'nowrap'}}>{key}</TableCell>
                                    <TableCell sx={{fontFamily: 'monospace', fontSize: '0.8rem', wordBreak: 'break-all'}}>{show(value)}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>}
                {!operation && (changes.length === 0
                    ? <Typography color='text.secondary' sx={{p: 2}}>
                        {event != null && event.action === 'DELETE' ? 'The object was deleted.' : 'No changes recorded.'}
                    </Typography>
                    : <Table size='small' stickyHeader>
                        <TableHead>
                            <TableRow>
                                <TableCell>Property</TableCell>
                                <TableCell>Before</TableCell>
                                <TableCell>After</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {changes.map(([property, [before, after]]) => (
                                <TableRow key={property}>
                                    <TableCell sx={{whiteSpace: 'nowrap'}}>{property}</TableCell>
                                    <TableCell sx={{fontFamily: 'monospace', fontSize: '0.8rem', wordBreak: 'break-all'}}>{show(before)}</TableCell>
                                    <TableCell sx={{fontFamily: 'monospace', fontSize: '0.8rem', wordBreak: 'break-all'}}>{show(after)}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>)}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} color='inherit'>Close</Button>
            </DialogActions>
        </Dialog>
    );
}

ChangesDialog.propTypes = {
    event: PropTypes.object,
    onClose: PropTypes.func.isRequired
};
