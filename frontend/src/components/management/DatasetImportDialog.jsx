import React, {useEffect, useState} from 'react';
import {
    Alert,
    Button,
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
import {handleErrors} from "../../utils/FetchHelper";

// Import of a dataset shipped with Databucket (e.g. DEMO) into an empty project
export default function DatasetImportDialog({project, onClose, onDone}) {

    const [datasets, setDatasets] = useState(null);
    const [selected, setSelected] = useState(null);
    const [working, setWorking] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        setSelected(null);
        setWorking(false);
        setError(null);
        if (project != null && datasets == null)
            fetch(getBaseUrl('manage/projects/datasets'), getGetOptions())
                .then(handleErrors)
                .then(setDatasets)
                .catch(e => setError(typeof e === 'string' ? e : e.message));
    }, [project, datasets]);

    const importDataset = () => {
        setWorking(true);
        setError(null);
        fetch(getBaseUrl(`manage/projects/${project.id}/datasets/${selected.id}`), getPostOptions())
            .then(handleErrors)
            .then(result => onDone(project, selected, result.rows))
            .catch(e => {
                setWorking(false);
                setError(typeof e === 'string' ? e : e.message);
            });
    }

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
                {datasets != null && datasets.length === 0 && <Alert severity="info">No datasets available.</Alert>}
                {datasets != null &&
                    <RadioGroup value={selected?.id ?? ''}
                                onChange={e => setSelected(datasets.find(d => d.id === e.target.value))}>
                        {datasets.map(dataset =>
                            <FormControlLabel
                                key={dataset.id}
                                value={dataset.id}
                                disabled={working}
                                sx={{alignItems: 'flex-start', mt: 1}}
                                control={<Radio size="small" color="secondary" sx={{pt: 0.25}}/>}
                                label={
                                    <div>
                                        <Typography fontWeight={600}>{dataset.name}</Typography>
                                        <Typography variant="body2" color="textSecondary">{dataset.description}</Typography>
                                        <Typography variant="caption" color="textSecondary">
                                            {`${dataset.buckets} buckets, ${dataset.views} views, ${dataset.rows.toLocaleString()} data rows`}
                                        </Typography>
                                    </div>
                                }
                            />
                        )}
                    </RadioGroup>
                }

                <Typography variant="body2" color="textSecondary" sx={{mt: 2}}>
                    Reserved data rows are reserved for you, and you join the teams of the dataset.
                </Typography>
                {working && <LinearProgress sx={{mt: 2}}/>}
                {error != null && <Alert severity="error" sx={{mt: 2}}>{error}</Alert>}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={working}>Cancel</Button>
                <Button variant="contained" onClick={importDataset} disabled={working || selected == null}>
                    Import
                </Button>
            </DialogActions>
        </Dialog>
    );
}
