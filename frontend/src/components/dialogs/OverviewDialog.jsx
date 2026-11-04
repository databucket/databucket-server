import React, {useContext, useEffect, useState} from 'react';
import {
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    IconButton,
    Link,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TableSortLabel,
    Tooltip,
    Typography,
    useTheme
} from "@mui/material";
import {Close, DeleteSweep, Inventory2, Lock, Refresh, WarningAmber} from "@mui/icons-material";
import AccessContext from "../../context/access/AccessContext";
import StyledIcon from "../utils/StyledIcon";
import {getBaseUrl} from "../../utils/UrlBuilder";
import {getButtonColor, getDeleteOptions, getGetOptions, getPostOptions} from "../../utils/MaterialTableHelper";
// rejects with the server's message as text (the MaterialTableHelper one rejects with an object, which can't be shown)
import {handleErrors} from "../../utils/FetchHelper";
import {getActiveProjectId, hasAdminRole} from "../../utils/ConfigurationStorage";
import {MessageBox} from "../utils/MessageBox";

const formatSize = (bytes) => {
    if (bytes < 1024)
        return `${bytes} B`;
    const units = ['kB', 'MB', 'GB', 'TB'];
    let value = bytes / 1024;
    let unit = 0;
    while (value >= 1024 && unit < units.length - 1) {
        value /= 1024;
        unit++;
    }
    return `${value < 10 ? value.toFixed(1) : Math.round(value)} ${units[unit]}`;
};

const formatNumber = (value) => value.toLocaleString();

// a vertical line before the History column: the data on the left, its history on the right
const SEPARATOR = {borderLeft: 1, borderLeftColor: 'divider'};

const formatDate = (value) => value != null ? new Date(value).toLocaleString() : '—';

// how a column is sorted; buckets without access have no reservations, history or last change
const SORT = {
    name: bucket => bucket.name.toLowerCase(),
    rows: bucket => bucket.rows,
    reserved: bucket => bucket.reserved,
    history: bucket => bucket.historySize ?? -1,
    lastModifiedAt: bucket => bucket.lastModifiedAt ?? '',
};

/**
 * The Overview of the project, opened from the side panel as a dialog (movable by its title, over the data):
 * the buckets the user has access to, how many rows they hold and how many are reserved, and how big their history is; and the
 * buckets visible on the Overview page the user has no access to, where the access can be asked for: the admins of
 * the project get an email and give the access in Settings (only with mail configured, otherwise there is no button).
 */
export default function OverviewDialog() {
    const theme = useTheme();
    const {buckets, addTab, projects} = useContext(AccessContext);
    const project = projects?.find(project => project.id === getActiveProjectId());
    const [open, setOpen] = useState(false);
    const [overview, setOverview] = useState(null);
    const [sort, setSort] = useState({by: 'name', asc: true});
    const [clearing, setClearing] = useState(null);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const admin = hasAdminRole();

    const showError = error => setMessageBox({open: true, severity: 'error', title: 'Error', message: error});

    const load = () => {
        fetch(getBaseUrl('overview'), getGetOptions())
            .then(handleErrors)
            .then(setOverview)
            .catch(showError);
    };

    // the numbers are read each time the dialog opens
    useEffect(() => {
        if (open)
            load();
        else
            setOverview(null);
    }, [open]);

    const openBucket = (item) => {
        const bucket = buckets?.find(bucket => bucket.id === item.id);
        if (bucket != null) {
            addTab(bucket);
            setOpen(false);
        } else
            // the access was granted or taken away since the page was loaded
            setMessageBox({open: true, severity: 'info', title: item.name,
                message: 'The access to this bucket has changed. Reload the page to see its data.'});
    };

    const askForAccess = (bucket) => {
        fetch(getBaseUrl(`overview/bucket/${encodeURIComponent(bucket.name)}/access-request`), getPostOptions({}))
            .then(handleErrors)
            .then(result => setMessageBox({open: true, severity: 'success', title: bucket.name, message: result.message}))
            .catch(showError);
    };

    const clearHistory = () => {
        const bucket = clearing;
        setClearing(null);
        fetch(getBaseUrl(`overview/bucket/${encodeURIComponent(bucket.name)}/history`), getDeleteOptions())
            .then(handleErrors)
            .then(result => {
                setMessageBox({open: true, severity: 'success', title: bucket.name, message: result.message});
                load();
            })
            .catch(showError);
    };

    const sortBy = (by) => setSort({by, asc: sort.by === by ? !sort.asc : true});

    const sorted = overview == null ? [] : [...overview].sort((a, b) => {
        const va = SORT[sort.by](a), vb = SORT[sort.by](b);
        return (va < vb ? -1 : va > vb ? 1 : 0) * (sort.asc ? 1 : -1);
    });

    const header = (by, label, align, sx) => (
        <TableCell align={align} sx={sx} sortDirection={sort.by === by ? (sort.asc ? 'asc' : 'desc') : false}>
            <TableSortLabel active={sort.by === by} direction={sort.by === by && !sort.asc ? 'desc' : 'asc'}
                            onClick={() => sortBy(by)}>
                {label}
            </TableSortLabel>
        </TableCell>
    );

    const bucketIcon = (bucket) => (
        <StyledIcon iconName={bucket.iconName} iconColor={bucket.iconColor} iconSvg={bucket.iconSvg}/>
    );

    const name = (bucket) => bucket.access
        ? <Tooltip title="Open the data">
            <Link component="button" underline="hover" color="inherit" onClick={() => openBucket(bucket)}
                  sx={{display: 'inline-flex', alignItems: 'center', gap: 1}}>
                {bucketIcon(bucket)}
                {bucket.name}
            </Link>
        </Tooltip>
        : <Tooltip title="No access to the data">
            <Box sx={{display: 'inline-flex', alignItems: 'center', gap: 1, color: 'text.disabled'}}>
                {bucketIcon(bucket)}
                {bucket.name}
                <Lock fontSize="inherit"/>
            </Box>
        </Tooltip>;

    const history = (bucket) => {
        if (!bucket.access)
            return '—';
        // not collected: just "off" (entries left from before are told in the tooltip; they can still be cleared)
        if (!bucket.history)
            return bucket.historyEntries > 0
                ? <Tooltip title={`Not collected; ${formatNumber(bucket.historyEntries)} entries (${formatSize(bucket.historySize)}) are left from before`}>
                    <Typography variant="body2" color="text.secondary" component="span">off</Typography>
                </Tooltip>
                : <Typography variant="body2" color="text.secondary">off</Typography>;
        return (
            <Box sx={{display: 'flex', alignItems: 'center', gap: 0.5, whiteSpace: 'nowrap'}}>
                <Tooltip title="Entries of all data rows (each version of a row is an entry) · size on disk">
                    <span>{formatNumber(bucket.historyEntries)} entries · {formatSize(bucket.historySize)}</span>
                </Tooltip>
                {bucket.historyLimit != null &&
                    <Tooltip title={`The latest ${bucket.historyLimit} entries of each row are kept (and the entry of its creation); older ones are removed every night`}>
                        <Typography component="span" variant="body2" color="text.secondary">· keep {formatNumber(bucket.historyLimit)}</Typography>
                    </Tooltip>}
                {bucket.historyLarge &&
                    <Tooltip title={admin
                        ? 'The history is large. Clear it to free the space.'
                        : 'The history is large. Ask an admin of the project to clear it.'}>
                        <WarningAmber color="warning" fontSize="small"/>
                    </Tooltip>}
            </Box>
        );
    };

    // clearing the history (admins, with access) or asking for access (without access, by email: only with mail)
    const action = (bucket) => {
        if (bucket.access)
            return admin && bucket.historyEntries > 0 &&
                <Tooltip title="Clear the whole history">
                    <IconButton size="small" onClick={() => setClearing(bucket)}>
                        <DeleteSweep fontSize="small"/>
                    </IconButton>
                </Tooltip>;
        if (!bucket.canAskForAccess)
            return null;
        return <Tooltip title="Email the admins of the project for access to the data">

            <Button size="small" variant="outlined" sx={{whiteSpace: 'nowrap'}}
                    onClick={() => askForAccess(bucket)}>
                Ask for access
            </Button>
        </Tooltip>;
    };

    return (
        <>
            <ListItem disablePadding>
                <ListItemButton onClick={() => setOpen(true)}>
                    <ListItemIcon>
                        <Inventory2 sx={{color: getButtonColor(theme)}}/>
                    </ListItemIcon>
                    <ListItemText primary={'Overview'}
                                  primaryTypographyProps={{style: {color: theme.palette.text.primary}}}/>
                </ListItemButton>
            </ListItem>
            <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="lg">
                <DialogTitle sx={{display: 'flex', alignItems: 'center', gap: 1, cursor: 'move'}}>
                    <Inventory2 color="action"/>
                    <span>Overview</span>
                    {project != null &&
                        <Typography component="span" variant="h6" color="text.secondary" sx={{fontWeight: 400}}>
                            · {project.name}
                        </Typography>}
                    <Box sx={{flexGrow: 1}}/>
                    <Tooltip title="Refresh">
                        <IconButton onClick={load} size="small"><Refresh/></IconButton>
                    </Tooltip>
                    <Tooltip title="Close">
                        <IconButton onClick={() => setOpen(false)} size="small"><Close/></IconButton>
                    </Tooltip>
                </DialogTitle>
                <DialogContent dividers sx={{p: 0}}>
                    {overview == null
                        ? <Box sx={{display: 'flex', justifyContent: 'center', p: 4}}><CircularProgress/></Box>
                        : <TableContainer>
                            <Table size="small" stickyHeader>
                                <TableHead>
                                    <TableRow>
                                        {header('name', 'Bucket')}
                                        <TableCell>Description</TableCell>
                                        {header('rows', 'Rows', 'right')}
                                        {header('reserved', 'Reserved', 'right')}
                                        {header('lastModifiedAt', 'Last change')}
                                        {header('history', 'Data history', undefined, SEPARATOR)}
                                        <TableCell/>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {sorted.map(bucket => (
                                        <TableRow key={bucket.id} hover>
                                            <TableCell sx={{whiteSpace: 'nowrap'}}>{name(bucket)}</TableCell>
                                            <TableCell sx={{color: 'text.secondary'}}>{bucket.description}</TableCell>
                                            <TableCell align="right">{formatNumber(bucket.rows)}</TableCell>
                                            <TableCell align="right">{formatNumber(bucket.reserved)}</TableCell>
                                            <TableCell sx={{whiteSpace: 'nowrap'}}>
                                                {bucket.access ? formatDate(bucket.lastModifiedAt) : '—'}
                                            </TableCell>
                                            <TableCell sx={SEPARATOR}>{history(bucket)}</TableCell>
                                            <TableCell align="right">{action(bucket)}</TableCell>
                                        </TableRow>
                                    ))}
                                    {sorted.length === 0 &&
                                        <TableRow>
                                            <TableCell colSpan={7} align="center" sx={{color: 'text.secondary'}}>
                                                There are no buckets to show in this project.
                                            </TableCell>
                                        </TableRow>}
                                </TableBody>
                            </Table>
                        </TableContainer>}
                </DialogContent>
            </Dialog>
            <Dialog open={clearing != null} onClose={() => setClearing(null)}>
                <DialogTitle>Clear history</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Remove the whole history of the bucket <b>{clearing?.name}</b>
                        {clearing != null && ` (${formatNumber(clearing.historyEntries)} entries, ${formatSize(clearing.historySize)})`}?
                        The data stay; their earlier versions can't be restored.
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setClearing(null)}>Cancel</Button>
                    <Button onClick={clearHistory} color="error">Clear history</Button>
                </DialogActions>
            </Dialog>
            <MessageBox config={messageBox} onClose={() => setMessageBox({...messageBox, open: false})}/>
        </>
    );
}
