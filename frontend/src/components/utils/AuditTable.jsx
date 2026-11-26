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
    Popover,
    Stack,
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
import {DateRange, FileDownload, FilterList, Refresh} from '@mui/icons-material';
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

// a Date as the value of a datetime-local input (local time, minutes)
const toLocalInput = date => {
    const pad = n => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

// a datetime-local value shown short, in the locale of the browser
const showLocal = local => new Date(local).toLocaleString(undefined, {dateStyle: 'short', timeStyle: 'short'});

const PERIODS = [['Last hour', 1], ['Last 24 hours', 24], ['Last 7 days', 24 * 7], ['Last 30 days', 24 * 30]];

// the filter row is shown or not, per table and browser (as in the other tables)
const filteringKey = endpoint => `audit-table-filtering-${endpoint}`;
const savedFiltering = endpoint => {
    try {
        return localStorage.getItem(filteringKey(endpoint)) === 'true';
    } catch (e) {
        return false;
    }
};
const saveFiltering = (endpoint, value) => {
    try {
        localStorage.setItem(filteringKey(endpoint), String(value));
    } catch (e) {
        // only a convenience
    }
};

const EMPTY_FILTER = {actor: ALL, type: ALL, action: ALL, project: ALL, from: '', to: ''};

/**
 * The changes of the settings (Settings > Activity) or of everything (Management > Audit log): newest first, filtered
 * by who, the type of the object, the action and the period; a row opens its changes, property by property.
 */
export default function AuditTable(props) {
    const theme = useTheme();
    const [filter, setFilter] = useState(EMPTY_FILTER);
    const [filtering, setFiltering] = useState(() => savedFiltering(props.endpoint));
    const [periodAnchor, setPeriodAnchor] = useState(null);
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

    // turning the filter row off clears the filters, as in the other tables
    const toggleFiltering = () => {
        if (filtering) {
            setFilter(EMPTY_FILTER);
            setPaging({...paging, page: 0});
        }
        saveFiltering(props.endpoint, !filtering);
        setFiltering(!filtering);
    };

    const filtered = Object.keys(EMPTY_FILTER).some(key => filter[key] !== EMPTY_FILTER[key]);

    const select = (name, options, width = 130) => (
        <TextField select size='small' variant='standard' value={filter[name]} sx={{width, display: 'block'}}
                   SelectProps={{displayEmpty: true}}
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

    const setPeriod = (from, to) => {
        setFilter({...filter, from, to});
        setPaging({...paging, page: 0});
    };

    // the period: a button in the When column, From and To (and a few periods) in a popup
    const periodText = filter.from || filter.to
        ? `${filter.from ? showLocal(filter.from) : '…'} – ${filter.to ? showLocal(filter.to) : '…'}`
        : 'Any time';
    const periodFilter = (
        <>
            <Button size='small' color={filter.from || filter.to ? 'secondary' : 'inherit'} startIcon={<DateRange/>}
                    onClick={e => setPeriodAnchor(e.currentTarget)}
                    sx={{display: 'flex', textTransform: 'none', px: 0, minWidth: 0, whiteSpace: 'nowrap',
                        fontWeight: 'normal'}}>
                {periodText}
            </Button>
            <Popover open={periodAnchor != null} anchorEl={periodAnchor} onClose={() => setPeriodAnchor(null)}
                     anchorOrigin={{vertical: 'bottom', horizontal: 'left'}}>
                <Stack spacing={2} sx={{p: 2, width: 260}}>
                    <TextField type='datetime-local' size='small' label='From' InputLabelProps={{shrink: true}}
                               value={filter.from} onChange={e => change('from', e.target.value)}/>
                    <TextField type='datetime-local' size='small' label='To' InputLabelProps={{shrink: true}}
                               value={filter.to} onChange={e => change('to', e.target.value)}/>
                    <Stack direction='row' sx={{flexWrap: 'wrap', gap: 1}}>
                        {PERIODS.map(([label, hours]) => (
                            <Chip key={label} size='small' variant='outlined' label={label}
                                  onClick={() => setPeriod(toLocalInput(new Date(Date.now() - hours * 3600000)), '')}/>
                        ))}
                    </Stack>
                    <Stack direction='row' sx={{justifyContent: 'flex-end', gap: 1}}>
                        <Button size='small' color='inherit' onClick={() => setPeriod('', '')}>Clear</Button>
                        <Button size='small' variant='contained' onClick={() => setPeriodAnchor(null)}>Close</Button>
                    </Stack>
                </Stack>
            </Popover>
        </>
    );

    return (
        <Box sx={{display: 'flex', flexDirection: 'column', height: props.height || getSettingsTableHeight()}}>
            <Toolbar sx={{gap: 1, alignItems: 'center'}}>
                <Typography variant='h6'>{props.title}</Typography>
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
                <Tooltip title='Filter'>
                    <IconButton onClick={toggleFiltering}>
                        <FilterList color={filtering && filtered ? 'secondary' : 'inherit'}/>
                    </IconButton>
                </Tooltip>
            </Toolbar>
            <TableContainer sx={{flex: 1}}>
                <Table size='small' stickyHeader>
                    <TableHead sx={{'& .MuiTableCell-head': {backgroundColor: getTableHeaderBackgroundColor(theme)}}}>
                        <TableRow sx={{'& .MuiTableCell-head': {verticalAlign: 'top'}}}>
                            <TableCell>When{filtering && periodFilter}</TableCell>
                            {projectOptions != null &&
                                <TableCell>Project{filtering && select('project', projectOptions, 150)}</TableCell>}
                            <TableCell>Who{filtering && select('actor', facets.actors)}</TableCell>
                            <TableCell>Source</TableCell>
                            <TableCell>Action{filtering && select('action', facets.actions || [], 150)}</TableCell>
                            <TableCell>Object{filtering && select('type', facets.types)}</TableCell>
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
