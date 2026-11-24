import React, {useState} from 'react';
import PropTypes from 'prop-types';
import {
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    TextField,
    Tooltip,
    Typography
} from '@mui/material';
import {MoreHoriz} from '@mui/icons-material';

export const RULES_PLACEHOLDER = '[["modifiedAt", "<", "@now-1d"]]';

// the rules as a list, or null (none); a text that is not valid JSON stays text
const asList = rules => {
    if (typeof rules !== 'string')
        return rules;
    try {
        return JSON.parse(rules);
    } catch (e) {
        return rules;
    }
};

// the conditions [left, operator, right], also those in groups ({"or": [...]}, {"!": ...})
export const countRules = rules => {
    const count = item => {
        if (Array.isArray(item))
            return item.length === 3 && typeof item[1] === 'string' && !Array.isArray(item[0])
                ? 1
                : item.reduce((sum, child) => sum + count(child), 0);
        if (item != null && typeof item === 'object')
            return Object.values(item).reduce((sum, child) => sum + count(child), 0);
        return 0;
    };
    const list = asList(rules);
    return Array.isArray(list) ? count(list) : 0;
};

// one item of the list per line
const formatRules = rules => {
    const list = asList(rules);
    if (list == null || (Array.isArray(list) && list.length === 0))
        return '';
    if (!Array.isArray(list))
        return typeof list === 'string' ? list : JSON.stringify(list);
    return '[\n' + list.map(item => '  ' + JSON.stringify(item)).join(',\n') + '\n]';
};

// the text as a list (null when empty), or an error message
const parseRules = text => {
    if (text.trim() === '')
        return {rules: null};
    let rules;
    try {
        rules = JSON.parse(text);
    } catch (e) {
        return {error: 'Not valid JSON: ' + e.message};
    }
    if (!Array.isArray(rules))
        return {error: 'The rules are a list, e.g. ' + RULES_PLACEHOLDER};
    return {rules: rules.length > 0 ? rules : null};
};

EditJobRulesDialog.propTypes = {
    // a list, or text while not yet valid
    rules: PropTypes.oneOfType([PropTypes.array, PropTypes.string]),
    onChange: PropTypes.func
};

/**
 * The extra rules of a job (Settings > Jobs): in the table the number of conditions, the rules in the tooltip; with
 * onChange (row edited) the button opens a dialog with the rules as text, checked on OK.
 */
export default function EditJobRulesDialog(props) {
    const [open, setOpen] = useState(false);
    const [text, setText] = useState('');
    const [error, setError] = useState(null);
    const count = countRules(props.rules);
    const formatted = formatRules(props.rules);

    const openDialog = () => {
        setText(formatted);
        setError(null);
        setOpen(true);
    };

    const save = () => {
        const result = parseRules(text);
        if (result.error != null) {
            setError(result.error);
            return;
        }
        props.onChange(result.rules);
        setOpen(false);
    };

    const tooltip = formatted === '' ? (props.onChange ? 'Define rules' : '') : (
        <Box component='pre' sx={{m: 0, fontFamily: 'monospace', fontSize: '0.75rem', whiteSpace: 'pre-wrap'}}>
            {formatted}
        </Box>
    );

    if (!props.onChange)
        return (
            <Tooltip title={tooltip} componentsProps={{tooltip: {sx: {maxWidth: 600}}}}>
                <span>{count}</span>
            </Tooltip>
        );

    return (
        <>
            <Tooltip title={tooltip} componentsProps={{tooltip: {sx: {maxWidth: 600}}}}>
                <Button endIcon={<MoreHoriz/>} onClick={openDialog} color='inherit' sx={{textTransform: 'none'}}>
                    {count}
                </Button>
            </Tooltip>
            <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth='md'>
                <DialogTitle>Job rules</DialogTitle>
                <DialogContent dividers sx={{display: 'flex', flexDirection: 'column', gap: 2}}>
                    <Typography variant='body2' color='text.secondary'>
                        The rows of the task filter AND these rules. A list of rules [left, operator, right],
                        groups as {'{"or": [...]}'}; dates also relative: @now, @now-2h (s, m, h, d, w).
                        Empty = no extra rules.
                    </Typography>
                    <TextField
                        multiline
                        minRows={10}
                        maxRows={25}
                        fullWidth
                        autoFocus
                        placeholder={RULES_PLACEHOLDER}
                        value={text}
                        onChange={e => {
                            setText(e.target.value);
                            setError(null);
                        }}
                        error={error != null}
                        helperText={error}
                        InputProps={{sx: {fontFamily: 'monospace', fontSize: '0.85rem'}}}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpen(false)} color='inherit'>Cancel</Button>
                    <Button onClick={save} variant='contained'>OK</Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
