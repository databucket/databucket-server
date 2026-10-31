import React, {useState} from 'react';
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
import {parseCsv, toImportRows} from "../../utils/CsvImport";
import {getDataUrl} from "../../utils/UrlBuilder";
import {getPostOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";

// errors listed in the dialog
const MAX_ERRORS = 10;

// Import of a CSV file (the view's Import feature): new rows from a file as the export writes it. The header maps
// the file's columns to the view's columns by their titles; the rows are checked before anything is saved, and they
// are saved in one request, in one transaction: all of them or none.
export default function ImportDataDialog({open, bucket, columns, onClose, onImported}) {

    const [fileName, setFileName] = useState(null);
    const [result, setResult] = useState(null);
    const [reserved, setReserved] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    const reset = () => {
        setFileName(null);
        setResult(null);
        setSaving(false);
        setError(null);
    }

    // each import starts with not reserved rows
    const close = () => {
        if (saving)
            return;
        reset();
        setReserved(false);
        onClose();
    }

    const onFileSelected = (event) => {
        const file = event.target.files[0];
        event.target.value = '';   // the same file can be chosen again
        if (file == null)
            return;
        reset();
        setFileName(file.name);
        file.text()
            .then(text => setResult(toImportRows(columns, parseCsv(text))))
            .catch(e => setError(`The file can't be read: ${e.message}`));
    }

    const importRows = () => {
        const rows = result.rows.map(row => ({...row, reserved}));
        setError(null);
        setSaving(true);
        fetch(getDataUrl(bucket) + '/multi', getPostOptions(rows))
            .then(handleErrors)
            .then(() => {
                reset();
                setReserved(false);
                onImported(rows.length);
            })
            .catch(e => {
                setSaving(false);
                setError(`Nothing has been imported: ${typeof e === 'string' ? e : e.message}`);
            });
    }

    const canImport = result != null && result.errors.length === 0 && result.rows.length > 0 && !saving;

    return (
        <Dialog open={open} onClose={close} maxWidth="sm" fullWidth>
            <DialogTitle>Import CSV</DialogTitle>
            <DialogContent>
                <Typography variant="body2" color="textSecondary" gutterBottom>
                    New rows from a CSV file as the export writes it: the first line holds the titles of the view's
                    columns. Id, reservation, owner and the created/modified columns are set by Databucket; a tag and
                    enum values are given by name.
                </Typography>
                <Button variant="outlined" component="label" disabled={saving} sx={{mt: 1, mb: 1}}>
                    Choose file
                    <input type="file" accept=".csv,text/csv" hidden onChange={onFileSelected}/>
                </Button>
                {fileName && <Typography variant="body2" component="span" sx={{ml: 2}}>{fileName}</Typography>}

                <RadioGroup
                    value={reserved ? 'reserved' : 'free'}
                    onChange={event => setReserved(event.target.value === 'reserved')}
                    sx={{mb: 1}}
                >
                    <FormControlLabel value="free" control={<Radio size="small" color="secondary"/>} disabled={saving}
                                      label="Not reserved (no owner)"/>
                    <FormControlLabel value="reserved" control={<Radio size="small" color="secondary"/>} disabled={saving}
                                      label="Reserved for me (I become the owner)"/>
                </RadioGroup>

                {result != null &&
                    <>
                        <Typography variant="body2">Rows: <b>{result.rows.length}</b></Typography>
                        <Typography variant="body2">
                            Imported columns: {result.imported.length > 0 ? result.imported.join(', ') : '-'}
                        </Typography>
                        {result.ignored.length > 0 &&
                            <Typography variant="body2" color="textSecondary">
                                Ignored columns: {result.ignored.join(', ')}
                            </Typography>
                        }
                        {result.errors.length > 0 &&
                            <Alert severity="error" sx={{mt: 2}}>
                                {result.errors.slice(0, MAX_ERRORS).map((e, i) => <div key={i}>{e}</div>)}
                                {result.errors.length > MAX_ERRORS &&
                                    <div>... and {result.errors.length - MAX_ERRORS} more</div>}
                            </Alert>
                        }
                    </>
                }
                {saving && <LinearProgress sx={{mt: 2}}/>}
                {error != null && <Alert severity="error" sx={{mt: 2}}>{error}</Alert>}
            </DialogContent>
            <DialogActions>
                <Button onClick={close} disabled={saving}>Cancel</Button>
                <Button onClick={importRows} disabled={!canImport} variant="contained">
                    {result != null && result.rows.length > 0 ? `Import ${result.rows.length} rows` : 'Import'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
