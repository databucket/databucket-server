import React, {useEffect, useRef, useState} from 'react';
import {
    Alert,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControlLabel,
    LinearProgress,
    Radio,
    RadioGroup,
    Typography
} from '@mui/material';
import {getBaseUrl} from "../../utils/UrlBuilder";
import {getGetOptions, getPostOptions} from "../../utils/MaterialTableHelper";
import {fetchHelper, handleErrors} from "../../utils/FetchHelper";
import {getToken} from "../../utils/ConfigurationStorage";

// The same limit as the server (DatasetService)
const MAX_FILE_SIZE = 50 * 1024 * 1024;
const FILE = '#file';

const summary = dataset =>
    `${dataset.buckets} buckets, ${dataset.views} views, ${dataset.rows.toLocaleString()} data rows`
    + (dataset.icons > 0 ? `, ${dataset.icons} icons` : '')
    + (dataset.changes > 0 ? `, ${dataset.changes.toLocaleString()} changes in the history` : '')
    + (dataset.users?.length > 0 ? `; users: ${dataset.users.join(', ')}` : '');

// what the dialog shows of a dataset file chosen from a disk, read in the browser; the server checks it on import
const describeFile = (file, text) => {
    const dataset = JSON.parse(text);
    if (dataset == null || typeof dataset !== 'object' || !Array.isArray(dataset.buckets))
        throw new Error("It isn't a dataset file");
    const rows = Object.values(dataset.data ?? {}).reduce((sum, list) => sum + (Array.isArray(list) ? list.length : 0), 0);
    return {
        fileName: file.name,
        text,
        name: dataset.name ?? file.name,
        description: dataset.description,
        buckets: dataset.buckets.length,
        views: (dataset.views ?? []).length,
        rows,
        icons: (dataset.icons ?? []).length,
        changes: (dataset.history?.changes ?? []).length,
        users: (dataset.users ?? []).map(user => user.username)
    };
}

// Import of a dataset into an empty project: a built-in one (e.g. DEMO), one from the server's folder or a file
export default function DatasetImportDialog({project, onClose, onDone}) {

    const [datasets, setDatasets] = useState(null);
    const [selected, setSelected] = useState(null);
    const [file, setFile] = useState(null);
    const [working, setWorking] = useState(false);
    const [error, setError] = useState(null);
    const fileInput = useRef(null);

    useEffect(() => {
        setSelected(null);
        setFile(null);
        setWorking(false);
        setError(null);
        if (project != null)
            // also the files added to the server's folder since the last time
            fetch(getBaseUrl('manage/projects/datasets'), getGetOptions())
                .then(handleErrors)
                .then(setDatasets)
                .catch(e => setError(typeof e === 'string' ? e : e.message));
    }, [project]);

    const onSelect = value => {
        setError(null);
        if (value === FILE) {
            setSelected(FILE);
            if (file == null)
                fileInput.current.click();
        } else
            setSelected(datasets.find(d => d.id === value));
    }

    const onFileChosen = async event => {
        const chosen = event.target.files[0];
        // the same file can be chosen again
        event.target.value = '';
        if (chosen == null)
            return;
        setSelected(FILE);
        setError(null);
        if (chosen.size > MAX_FILE_SIZE) {
            setFile(null);
            setError(`${chosen.name}: the file is larger than ${MAX_FILE_SIZE / 1024 / 1024} MB`);
            return;
        }
        try {
            setFile(describeFile(chosen, await chosen.text()));
        } catch (e) {
            setFile(null);
            setError(`${chosen.name}: ${e instanceof SyntaxError ? "it isn't a JSON file" : e.message}`);
        }
    }

    const importDataset = () => {
        setWorking(true);
        setError(null);
        const request = selected === FILE
            ? fetch(getBaseUrl(`manage/projects/${project.id}/dataset`), {
                method: 'POST',
                headers: fetchHelper(getToken()),
                body: file.text
            })
            : fetch(getBaseUrl(`manage/projects/${project.id}/datasets/${selected.id}`), getPostOptions());
        request
            .then(handleErrors)
            .then(result => onDone(project, selected === FILE ? file : selected, result.rows))
            .catch(e => {
                setWorking(false);
                setError(typeof e === 'string' ? e : e.message);
            });
    }

    const canImport = !working && (selected === FILE ? file != null : selected != null);

    return (
        <Dialog open={project != null} onClose={() => !working && onClose()} maxWidth="sm" fullWidth>
            <DialogTitle>Import dataset</DialogTitle>
            <DialogContent>
                <Typography gutterBottom>
                    {'Fill the empty project '}
                    <Typography component="span" color="secondary">{project?.name}</Typography>
                    {' with the buckets, views and data of a dataset:'}
                </Typography>

                {datasets == null && error == null && <LinearProgress sx={{my: 2}}/>}
                {datasets != null &&
                    <RadioGroup value={selected === FILE ? FILE : selected?.id ?? ''} onChange={e => onSelect(e.target.value)}>
                        {datasets.map(dataset =>
                            <FormControlLabel
                                key={dataset.id}
                                value={dataset.id}
                                disabled={working}
                                sx={{alignItems: 'flex-start', mt: 1}}
                                control={<Radio size="small" color="secondary" sx={{pt: 0.25}}/>}
                                label={
                                    <div>
                                        <Typography fontWeight={600} component="span">{dataset.name}</Typography>
                                        {dataset.source === 'server' &&
                                            <Chip label="server" size="small" variant="outlined" sx={{ml: 1, height: 20}}/>}
                                        <Typography variant="body2" color="textSecondary">{dataset.description}</Typography>
                                        <Typography variant="caption" color="textSecondary">{summary(dataset)}</Typography>
                                    </div>
                                }
                            />
                        )}
                        <FormControlLabel
                            value={FILE}
                            disabled={working}
                            sx={{alignItems: 'flex-start', mt: 1}}
                            control={<Radio size="small" color="secondary" sx={{pt: 0.25}}/>}
                            label={
                                <div>
                                    <Typography fontWeight={600} component="span">From a file…</Typography>
                                    {file == null
                                        ? <Typography variant="body2" color="textSecondary">
                                            A dataset file from your disk, e.g. a project exported before
                                        </Typography>
                                        : <>
                                            <Typography variant="body2">{file.name}
                                                <Typography component="span" variant="body2" color="textSecondary">
                                                    {` (${file.fileName})`}
                                                </Typography>
                                            </Typography>
                                            {file.description &&
                                                <Typography variant="body2" color="textSecondary">{file.description}</Typography>}
                                            <Typography variant="caption" color="textSecondary">{summary(file)}</Typography>
                                            <div>
                                                <Button size="small" sx={{p: 0, minWidth: 0}} disabled={working}
                                                        onClick={() => fileInput.current.click()}>
                                                    Choose another file
                                                </Button>
                                            </div>
                                        </>
                                    }
                                </div>
                            }
                        />
                    </RadioGroup>
                }
                <input ref={fileInput} type="file" accept=".json,application/json" style={{display: 'none'}}
                       onChange={onFileChosen}/>

                <Typography variant="body2" color="textSecondary" sx={{mt: 2}}>
                    Reserved data rows are reserved for you, and you join the teams of the dataset. The users of
                    the dataset are created if they don't exist yet, and get access to the project.
                </Typography>
                {working && <LinearProgress sx={{mt: 2}}/>}
                {error != null && <Alert severity="error" sx={{mt: 2, whiteSpace: 'pre-line'}}>{error}</Alert>}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={working}>Cancel</Button>
                <Button variant="contained" onClick={importDataset} disabled={!canImport}>
                    Import
                </Button>
            </DialogActions>
        </Dialog>
    );
}
