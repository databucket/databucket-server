import React, {useState} from 'react';
import PropTypes from 'prop-types';
import {
    Autocomplete,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    MenuItem,
    Stack,
    TextField,
    Tooltip,
    Typography
} from '@mui/material';
import {AddCircleOutline, DeleteOutline, MoreHoriz} from '@mui/icons-material';

export const MAX_INDEXED_PROPERTIES = 5;
// the same as IndexedPropertyDto.PATH_PATTERN on the server
const PATH_PATTERN = /^\$(\.[\p{L}\p{N}_\- ]+)+$/u;

EditIndexedPropertiesDialog.propTypes = {
    rowData: PropTypes.object.isRequired,
    classes: PropTypes.array,
    onChange: PropTypes.func.isRequired
}

const pathError = (items, index) => {
    const path = items[index].path.trim();
    if (!PATH_PATTERN.test(path))
        return 'A property like $.a.b (letters, digits, _, - and spaces)';
    if (items.findIndex(item => item.path.trim() === path) !== index)
        return 'Indexed twice';
    return null;
}

/**
 * The indexed properties of a bucket: a database index for each, so that rules on them are fast in a big bucket.
 */
export default function EditIndexedPropertiesDialog(props) {
    const [open, setOpen] = useState(false);
    const [items, setItems] = useState([]);
    const current = props.rowData.indexedProperties || [];

    // the properties of the class of the bucket, as suggestions
    const dataClass = props.classes != null ? props.classes.find(c => String(c.id) === String(props.rowData.classId)) : null;
    const classFields = dataClass != null && dataClass.configuration != null
        ? dataClass.configuration.filter(field => field.path != null && field.path.startsWith('$.'))
        : [];

    const handleOpen = () => {
        setItems(current.map(item => ({...item})));
        setOpen(true);
    }

    const invalid = items.some((item, index) => pathError(items, index) != null);

    const handleSave = () => {
        props.onChange(items.map(item => ({path: item.path.trim(), type: item.type})));
        setOpen(false);
    }

    const setItem = (index, changes) => setItems(items.map((item, i) => i === index ? {...item, ...changes} : item));

    const setPath = (index, path) => {
        const field = classFields.find(f => f.path === path);
        if (field != null)
            setItem(index, {path, type: field.type === 'numeric' ? 'number' : 'text'});
        else
            setItem(index, {path: path || ''});
    }

    return (
        <div>
            <Tooltip title={'Edit indexed properties'}>
                <Button endIcon={<MoreHoriz/>} onClick={handleOpen} style={{textTransform: 'none'}} color={'inherit'}>
                    {`${current.length}`}
                </Button>
            </Tooltip>
            <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth='sm'>
                <DialogTitle>Indexed properties</DialogTitle>
                <DialogContent dividers>
                    <Typography variant='body2' color='text.secondary' sx={{mb: 2}}>
                        A database index for each property makes the rules on it fast in a big bucket: text for ==,
                        in and like 'A%', number for ==, &lt;, &gt; and between. At most {MAX_INDEXED_PROPERTIES}.
                        Saving builds the indexes, which takes a while for many rows.
                    </Typography>
                    <Stack spacing={1.5}>
                        {items.map((item, index) => {
                            const error = pathError(items, index);
                            return (
                                <Stack key={index} direction='row' spacing={1} alignItems='flex-start'>
                                    <Autocomplete
                                        freeSolo
                                        fullWidth
                                        options={classFields.map(f => f.path)}
                                        renderOption={(optionProps, option) => {
                                            const field = classFields.find(f => f.path === option);
                                            return <li {...optionProps} key={option}>{option}{field != null && field.title ?
                                                <Typography component='span' variant='body2' color='text.secondary' sx={{ml: 1}}>{field.title}</Typography> : null}</li>;
                                        }}
                                        inputValue={item.path}
                                        onInputChange={(e, value) => setPath(index, value)}
                                        renderInput={params => <TextField {...params} size='small' label='Property'
                                                                          placeholder='$.account.status'
                                                                          error={item.path !== '' && error != null}
                                                                          helperText={item.path !== '' ? error : null}/>}
                                    />
                                    <TextField select size='small' label='Type' value={item.type} sx={{minWidth: 120}}
                                               onChange={e => setItem(index, {type: e.target.value})}>
                                        <MenuItem value='text'>text</MenuItem>
                                        <MenuItem value='number'>number</MenuItem>
                                    </TextField>
                                    <Tooltip title='Remove'>
                                        <IconButton onClick={() => setItems(items.filter((_, i) => i !== index))}>
                                            <DeleteOutline/>
                                        </IconButton>
                                    </Tooltip>
                                </Stack>
                            );
                        })}
                    </Stack>
                    <Button startIcon={<AddCircleOutline/>} sx={{mt: 2}} disabled={items.length >= MAX_INDEXED_PROPERTIES}
                            onClick={() => setItems([...items, {path: '', type: 'text'}])}>
                        Add property
                    </Button>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpen(false)}>Cancel</Button>
                    <Button onClick={handleSave} disabled={invalid} variant='contained'>OK</Button>
                </DialogActions>
            </Dialog>
        </div>
    );
}
