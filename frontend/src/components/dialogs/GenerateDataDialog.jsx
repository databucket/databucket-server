import React, {useEffect, useRef, useState} from 'react';
import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControlLabel,
    IconButton,
    LinearProgress,
    Link,
    MenuItem,
    Radio,
    RadioGroup,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Tooltip,
    Typography
} from '@mui/material';
import {Add, DeleteOutline} from '@mui/icons-material';
import {getDataUrl} from "../../utils/UrlBuilder";
import {getPostOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {
    emptyRow,
    KIND_DATE,
    KIND_DATETIME,
    KIND_NUMBER,
    KIND_ONE_OF,
    KINDS,
    LOCALES,
    rowsFromColumns,
    toSpec,
    withKind
} from "../../utils/GenerateSpec";

const MAX_ROWS = 10000;
const DATAFAKER_URL = 'https://www.datafaker.net/documentation/providers/';

// the last generators of each bucket stay in this browser (a convenience: without storage the dialog starts from
// the view's columns)
const storageKey = bucket => `databucket.generate.${bucket.id}`;
const load = bucket => {
    try {
        const text = localStorage.getItem(storageKey(bucket));
        return text != null ? JSON.parse(text) : null;
    } catch (e) {
        return null;
    }
}
const save = (bucket, settings) => {
    try {
        localStorage.setItem(storageKey(bucket), JSON.stringify(settings));
    } catch (e) {
        // not remembered
    }
}

// the settings of a generator, in one table cell (the header says what the columns are, placeholders what to type)
const GeneratorInputs = ({row, onChange, disabled}) => {
    const field = (name, placeholder, props = {}) =>
        <TextField size="small" variant="standard" placeholder={placeholder} value={row[name] ?? ''}
                   disabled={disabled} inputProps={{'aria-label': placeholder}}
                   onChange={e => onChange({...row, [name]: e.target.value})} {...props}/>;
    const dash = <Typography variant="body2" color="textSecondary">–</Typography>;
    switch (row.kind) {
        case KIND_NUMBER:
            return <Stack direction="row" spacing={1} alignItems="center">
                {field('min', 'min', {sx: {width: 70}})}
                {dash}
                {field('max', 'max', {sx: {width: 70}})}
                <Typography variant="body2" color="textSecondary" sx={{pl: 1}}>decimals</Typography>
                {field('decimals', 'decimals', {sx: {width: 40}})}
            </Stack>;
        case KIND_ONE_OF:
            return field('values', 'values, separated with commas', {fullWidth: true});
        case KIND_DATE:
        case KIND_DATETIME:
            return <Stack direction="row" spacing={1} alignItems="center">
                {field('from', 'from', {type: 'date', sx: {width: 135}})}
                {dash}
                {field('to', 'to', {type: 'date', sx: {width: 135}})}
            </Stack>;
        default:
            return field('text', 'text, {n} or #{expression}', {fullWidth: true});
    }
}

// a value of the preview by the path of a generator: "$.address.city" -> row.address.city
const valueAt = (row, path) => {
    const value = path.substring(2).split('.').reduce((obj, node) => obj != null ? obj[node] : undefined, row);
    return value == null ? '' : typeof value === 'object' ? JSON.stringify(value) : String(value);
}

const cellSx = {py: 0.5, px: 1};

// Generate data (the view's Import feature): random rows made on the server from a generator per property, inserted
// in one request, all of them or none. The generators are proposed from the view's columns (types, enums, names).
export default function GenerateDataDialog({open, bucket, columns, tags, onClose, onGenerated}) {

    const [rows, setRows] = useState([]);
    const [count, setCount] = useState('100');
    const [locale, setLocale] = useState('en');
    const [seed, setSeed] = useState('');
    const [tagId, setTagId] = useState('');
    const [reserved, setReserved] = useState(false);
    const [preview, setPreview] = useState(null);
    const [working, setWorking] = useState(false);
    const [error, setError] = useState(null);
    const previewRef = useRef(null);

    // the preview is below the settings: show it
    useEffect(() => {
        if (preview != null)
            previewRef.current?.scrollIntoView({block: "nearest"});
    }, [preview]);

    useEffect(() => {
        if (!open || bucket == null)
            return;
        const saved = load(bucket);
        setRows(saved?.rows ?? rowsFromColumns(columns));
        setCount(saved?.count ?? '100');
        setLocale(saved?.locale ?? 'en');
        setSeed(saved?.seed ?? '');
        setTagId('');
        setReserved(false);
        setPreview(null);
        setError(null);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open, bucket]);

    const close = () => {
        if (!working)
            onClose();
    }

    const changeRow = (index, row) => {
        setRows(rows.map((r, i) => i === index ? row : r));
        setPreview(null);
    }

    // values of the new kind not given yet get defaults
    const changeKind = (index, kind) => changeRow(index, withKind(rows[index], kind));

    const request = () => {
        const n = Number(count);
        if (!Number.isInteger(n) || n < 1 || n > MAX_ROWS)
            throw `Give the number of rows, 1..${MAX_ROWS}`;
        if (seed !== '' && !Number.isInteger(Number(seed)))
            throw 'The seed is a whole number';
        return {
            fields: toSpec(rows),
            count: n,
            locale,
            seed: seed !== '' ? Number(seed) : null,
            tagId: tagId !== '' ? tagId : null,
            reserved
        };
    }

    const send = (previewOnly) => {
        let body;
        try {
            body = request();
        } catch (message) {
            setError(message);
            return;
        }
        setError(null);
        setWorking(true);
        fetch(getDataUrl(bucket) + '/generate' + (previewOnly ? '?preview=true' : ''), getPostOptions(body))
            .then(handleErrors)
            .then(result => {
                setWorking(false);
                save(bucket, {rows, count, locale, seed});
                if (previewOnly)
                    setPreview(result.rows);
                else
                    onGenerated(result.inserted);
            })
            .catch(e => {
                setWorking(false);
                setError((previewOnly ? '' : 'Nothing has been generated: ') + (typeof e === 'string' ? e : e.message));
            });
    }

    const reset = () => {
        setRows(rowsFromColumns(columns));
        setPreview(null);
        setError(null);
    }

    return (
        <Dialog open={open} onClose={close} maxWidth="md" fullWidth>
            <DialogTitle>Generate data</DialogTitle>
            <DialogContent>
                <Typography variant="body2" color="textSecondary" gutterBottom>
                    Random rows made by Databucket, a generator per property. A text may hold <b>{'{n}'}</b>, the
                    number of the row (unique, it continues after the last row of the bucket), and Datafaker
                    expressions such as <b>{'#{Name.firstName}'}</b> or <b>{"#{numerify '###-###'}"}</b> (see
                    the <Link href={DATAFAKER_URL} target="_blank" rel="noopener">providers</Link>). Any other text is
                    the same in every row.
                </Typography>

                <TableContainer sx={{mt: 2}}>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell sx={{...cellSx, width: 190}}>Property</TableCell>
                                <TableCell sx={{...cellSx, width: 150}}>Generator</TableCell>
                                <TableCell sx={cellSx}>Settings</TableCell>
                                <TableCell sx={{...cellSx, width: 40}}/>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {rows.map((row, index) =>
                                <TableRow key={index} hover>
                                    <TableCell sx={cellSx}>
                                        {row.title != null &&
                                            <Typography variant="caption" color="textSecondary" component="div">
                                                {row.title}
                                            </Typography>}
                                        <TextField size="small" variant="standard" value={row.path} disabled={working}
                                                   fullWidth inputProps={{'aria-label': 'Property path'}}
                                                   onChange={e => changeRow(index, {...row, path: e.target.value})}/>
                                    </TableCell>
                                    <TableCell sx={cellSx}>
                                        <TextField select size="small" variant="standard" value={row.kind} fullWidth
                                                   disabled={working} inputProps={{'aria-label': 'Generator'}}
                                                   onChange={e => changeKind(index, e.target.value)}>
                                            {KINDS.map(k => <MenuItem key={k.value} value={k.value}>{k.label}</MenuItem>)}
                                        </TextField>
                                    </TableCell>
                                    <TableCell sx={cellSx}>
                                        <GeneratorInputs row={row} disabled={working}
                                                         onChange={r => changeRow(index, r)}/>
                                    </TableCell>
                                    <TableCell sx={cellSx} align="right">
                                        <Tooltip title="Remove">
                                            <span>
                                                <IconButton size="small" disabled={working}
                                                            onClick={() => {
                                                                setRows(rows.filter((r, i) => i !== index));
                                                                setPreview(null);
                                                            }}>
                                                    <DeleteOutline fontSize="small"/>
                                                </IconButton>
                                            </span>
                                        </Tooltip>
                                    </TableCell>
                                </TableRow>
                            )}
                        </TableBody>
                    </Table>
                </TableContainer>
                <Stack direction="row" spacing={1} sx={{mt: 1}}>
                    <Button size="small" startIcon={<Add/>} disabled={working}
                            onClick={() => setRows([...rows, emptyRow()])}>Add property</Button>
                    <Button size="small" disabled={working} onClick={reset}>Propose from the columns</Button>
                </Stack>

                <Stack direction="row" spacing={2} sx={{mt: 2}} flexWrap="wrap" useFlexGap alignItems="flex-end">
                    <TextField size="small" variant="standard" label="Rows" value={count} disabled={working}
                               helperText={`1..${MAX_ROWS}`} sx={{width: 90}}
                               onChange={e => setCount(e.target.value)}/>
                    <TextField select size="small" variant="standard" label="Language" value={locale}
                               disabled={working} helperText=" " sx={{width: 90}}
                               onChange={e => {
                                   setLocale(e.target.value);
                                   setPreview(null);
                               }}>
                        {LOCALES.map(l => <MenuItem key={l} value={l}>{l}</MenuItem>)}
                    </TextField>
                    <TextField size="small" variant="standard" label="Seed" value={seed} disabled={working}
                               helperText="the same rows again" sx={{width: 130}}
                               onChange={e => {
                                   setSeed(e.target.value);
                                   setPreview(null);
                               }}/>
                    {tags != null && tags.length > 0 &&
                        <TextField select size="small" variant="standard" label="Tag" value={tagId}
                                   disabled={working} helperText=" " sx={{width: 150}}
                                   onChange={e => setTagId(e.target.value)}>
                            <MenuItem value=""><em>none</em></MenuItem>
                            {tags.map(t => <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>)}
                        </TextField>
                    }
                </Stack>

                <RadioGroup row value={reserved ? 'reserved' : 'free'} sx={{mt: 1}}
                            onChange={event => setReserved(event.target.value === 'reserved')}>
                    <FormControlLabel value="free" control={<Radio size="small" color="secondary"/>} disabled={working}
                                      label="Not reserved"/>
                    <FormControlLabel value="reserved" control={<Radio size="small" color="secondary"/>}
                                      disabled={working} label="Reserved for me"/>
                </RadioGroup>

                {preview != null &&
                    <TableContainer ref={previewRef} sx={{
                        mt: 1, maxHeight: 260, border: 1, borderColor: 'divider', borderRadius: 1
                    }}>
                        <Table size="small" stickyHeader>
                            <TableHead>
                                <TableRow>
                                    {rows.map((row, i) =>
                                        <TableCell key={i} sx={{...cellSx, whiteSpace: 'nowrap'}}>
                                            {row.title ?? row.path}
                                        </TableCell>)}
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {preview.map((data, r) =>
                                    <TableRow key={r} hover>
                                        {rows.map((row, i) =>
                                            <TableCell key={i} sx={{...cellSx, whiteSpace: 'nowrap'}}>
                                                {valueAt(data, row.path)}
                                            </TableCell>)}
                                    </TableRow>)}
                            </TableBody>
                        </Table>
                    </TableContainer>
                }
                {working && <LinearProgress sx={{mt: 2}}/>}
                {error != null && <Alert severity="error" sx={{mt: 2}}>{error}</Alert>}
            </DialogContent>
            <DialogActions>
                <Button onClick={close} disabled={working}>Cancel</Button>
                <Button onClick={() => send(true)} disabled={working || rows.length === 0}>Preview</Button>
                <Button onClick={() => send(false)} disabled={working || rows.length === 0} variant="contained">
                    {Number.isInteger(Number(count)) && Number(count) > 0 ? `Generate ${count} rows` : 'Generate'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
