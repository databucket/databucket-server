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
import {Close, DeleteSweep, Inventory2, Refresh, WarningAmber} from "@mui/icons-material";
import AccessContext from "../../context/access/AccessContext";
import StyledIcon from "../utils/StyledIcon";
import {getBaseUrl} from "../../utils/UrlBuilder";
import {getButtonColor, getDeleteOptions, getGetOptions, handleErrors} from "../../utils/MaterialTableHelper";
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

const formatDate = (value) => value != null ? new Date(value).toLocaleString() : '—';

// how a column is sorted
const SORT = {
    name: bucket => bucket.name.toLowerCase(),
    rows: bucket => bucket.rows,
    free: bucket => bucket.free,
    reserved: bucket => bucket.reservedByMe + bucket.reservedByOthers,
    history: bucket => bucket.historySize,
    lastModifiedAt: bucket => bucket.lastModifiedAt ?? '',
};

/**
 * The Overview of the project, opened from the side panel as a dialog (movable by its title, over the data):
 * the buckets the user has access to, what they hold, how much is free, and how big their history is.
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

    const load = () => {
        fetch(getBaseUrl('overview'), getGetOptions())
            .then(handleErrors)
            .then(setOverview)
            .catch(error => setMessageBox({open: true, severity: 'error', title: 'Error', message: error}));
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
            // the access was taken away since the page was loaded
            setMessageBox({open: true, severity: 'info', title: item.name, message: 'You have no access to the data of this bucket.'});
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
            .catch(error => setMessageBox({open: true, severity: 'error', title: 'Error', message: error}));
    };

    const sortBy = (by) => setSort({by, asc: sort.by === by ? !sort.asc : true});

    const sorted = overview == null ? [] : [...overview].sort((a, b) => {
        const va = SORT[sort.by](a), vb = SORT[sort.by](b);
        return (va < vb ? -1 : va > vb ? 1 : 0) * (sort.asc ? 1 : -1);
    });

    const header = (by, label, align) => (
        <TableCell align={align} sortDirection={sort.by === by ? (sort.asc ? 'asc' : 'desc') : false}>
            <TableSortLabel active={sort.by === by} direction={sort.by === by && !sort.asc ? 'desc' : 'asc'}
                            onClick={() => sortBy(by)}>
                {label}
            </TableSortLabel>
        </TableCell>
    );

    const history = (bucket) => {
        if (!bucket.history && bucket.historyEntries === 0)
            return <Typography variant="body2" color="text.secondary">off</Typography>;
        return (
            <Box sx={{display: 'flex', alignItems: 'center', gap: 0.5, whiteSpace: 'nowrap'}}>
                <span>{bucket.history ? 'on' : 'off'} · {formatNumber(bucket.historyEntries)} · {formatSize(bucket.historySize)}</span>
                {bucket.historyLarge &&
                    <Tooltip title={admin
                        ? 'The history is large. Clear it to free the space.'
                        : 'The history is large. Ask an admin of the project to clear it.'}>
                        <WarningAmber color="warning" fontSize="small"/>
                    </Tooltip>}
            </Box>
        );
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
                                        {header('free', 'Free', 'right')}
                                        {header('reserved', 'Reserved (me / others)', 'right')}
                                        {header('history', 'History')}
                                        {header('lastModifiedAt', 'Last change')}
                                        {admin && <TableCell/>}
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {sorted.map(bucket => (
                                        <TableRow key={bucket.id} hover>
                                            <TableCell sx={{whiteSpace: 'nowrap'}}>
                                                <Tooltip title="Open the data">
                                                    <Link component="button" underline="hover" color="inherit"
                                                          onClick={() => openBucket(bucket)}
                                                          sx={{display: 'inline-flex', alignItems: 'center', gap: 1}}>
                                                        <StyledIcon iconName={bucket.iconName}
                                                                    iconColor={bucket.iconColor}
                                                                    iconSvg={bucket.iconSvg}/>
                                                        {bucket.name}
                                                    </Link>
                                                </Tooltip>
                                            </TableCell>
                                            <TableCell sx={{color: 'text.secondary'}}>{bucket.description}</TableCell>
                                            <TableCell align="right">{formatNumber(bucket.rows)}</TableCell>
                                            <TableCell align="right">{formatNumber(bucket.free)}</TableCell>
                                            <TableCell align="right">
                                                {formatNumber(bucket.reservedByMe)} / {formatNumber(bucket.reservedByOthers)}
                                            </TableCell>
                                            <TableCell>{history(bucket)}</TableCell>
                                            <TableCell sx={{whiteSpace: 'nowrap'}}>{formatDate(bucket.lastModifiedAt)}</TableCell>
                                            {admin &&
                                                <TableCell padding="checkbox">
                                                    {bucket.historyEntries > 0 &&
                                                        <Tooltip title="Clear the whole history">
                                                            <IconButton size="small" onClick={() => setClearing(bucket)}>
                                                                <DeleteSweep fontSize="small"/>
                                                            </IconButton>
                                                        </Tooltip>}
                                                </TableCell>}
                                        </TableRow>
                                    ))}
                                    {sorted.length === 0 &&
                                        <TableRow>
                                            <TableCell colSpan={8} align="center" sx={{color: 'text.secondary'}}>
                                                You have no access to any bucket of this project.
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
