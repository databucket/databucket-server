import React, {useState} from 'react';
import PropTypes from 'prop-types';
import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Tab,
    Tabs,
    TextField,
    Tooltip,
    Typography,
    useTheme
} from '@mui/material';
import {MoreHoriz} from '@mui/icons-material';
import {Query, Utils as QbUtils} from '@react-awesome-query-builder/mui';
import {createConfig, getInitialTree, renderBuilder} from '../utils/QueryBuilderHelper';
import {logicToRules} from '../../utils/LogicToRules';
import {rulesToLogic, sameLogic} from '../../utils/RulesToLogic';

export const RULES_PLACEHOLDER = '[["modifiedAt", "<", "@now-1d"]]';

const BUILDER = 0;
const JSON_TAB = 1;

const NOT_IN_BUILDER = 'The builder can\'t show these rules (e.g. an operator like similar or ~, a property that is'
    + ' not in the class of the bucket, a value written differently than the builder writes it): edit them as JSON.';

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

// the tree of the builder for the rules, or null when the builder can't show them as they are
const treeOf = (rules, config) => {
    const logic = rulesToLogic(rules);
    if (logic == null)
        return null;
    try {
        const tree = QbUtils.checkTree(getInitialTree(logic, null, config), config);
        return sameLogic(logic, QbUtils.jsonLogicFormat(tree, config).logic) ? tree : null;
    } catch (e) {
        return null;
    }
};

// the rules of the tree (null when none), or an error message
const rulesOfTree = (tree, config) => {
    const logic = QbUtils.jsonLogicFormat(tree, config).logic;
    const rules = logicToRules(logic);
    if (rules == null)
        return {error: 'A condition of the builder can\'t be saved as rules (e.g. starts with, not any in):'
                + ' change it or edit the rules as JSON.'};
    return {rules: rules.length > 0 ? rules : null};
};

EditJobRulesDialog.propTypes = {
    // a list, or text while not yet valid
    rules: PropTypes.oneOfType([PropTypes.array, PropTypes.string]),
    onChange: PropTypes.func,
    // for the builder: the properties of the class of the job's bucket, its tags, the users, the enums
    properties: PropTypes.array,
    tags: PropTypes.array,
    users: PropTypes.array,
    enums: PropTypes.array
};

/**
 * The extra rules of a job (Settings > Jobs): in the table the number of conditions, the rules in the tooltip; with
 * onChange (row edited) the button opens a dialog with the rules in the query builder (fields of the bucket's class)
 * or as JSON text; saved as the list of rules either way. Rules the builder can't show open as JSON.
 */
export default function EditJobRulesDialog(props) {
    const theme = useTheme();
    const [open, setOpen] = useState(false);
    const [tab, setTab] = useState(BUILDER);
    const [text, setText] = useState('');
    const [builder, setBuilder] = useState(null); // {config, tree}
    const [error, setError] = useState(null);
    const [note, setNote] = useState(null);
    const count = countRules(props.rules);
    const formatted = formatRules(props.rules);

    const makeConfig = () => createConfig(props.properties || [], props.tags || [], props.users || [],
        props.enums || [], theme);

    const openDialog = () => {
        const config = makeConfig();
        const list = asList(props.rules);
        const tree = typeof list === 'string' ? null : treeOf(list, config);
        setText(formatted);
        setError(null);
        setBuilder({config, tree: tree || treeOf(null, config)});
        setNote(tree == null ? NOT_IN_BUILDER : null);
        setTab(tree == null ? JSON_TAB : BUILDER);
        setOpen(true);
    };

    const changeTab = (event, newTab) => {
        if (newTab === tab)
            return;
        if (newTab === JSON_TAB) {
            const result = rulesOfTree(builder.tree, builder.config);
            if (result.error != null) {
                setError(result.error);
                return;
            }
            setText(formatRules(result.rules));
        } else {
            const result = parseRules(text);
            if (result.error != null) {
                setError(result.error);
                return;
            }
            const tree = treeOf(result.rules, builder.config);
            if (tree == null) {
                setError(NOT_IN_BUILDER);
                return;
            }
            setBuilder({...builder, tree});
        }
        setError(null);
        setNote(null);
        setTab(newTab);
    };

    const save = () => {
        const result = tab === BUILDER ? rulesOfTree(builder.tree, builder.config) : parseRules(text);
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
            <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth='lg'>
                <DialogTitle sx={{display: 'flex', alignItems: 'center', gap: 2}}>
                    <Box sx={{flex: 1}}>Job rules</Box>
                    <Tabs value={tab} onChange={changeTab} textColor='secondary' indicatorColor='secondary'>
                        <Tab label='Builder'/>
                        <Tab label='JSON'/>
                    </Tabs>
                    <Box sx={{flex: 1}}/>
                </DialogTitle>
                <DialogContent dividers sx={{display: 'flex', flexDirection: 'column', gap: 2, minHeight: '50vh'}}>
                    <Typography variant='body2' color='text.secondary'>
                        The rows of the task filter AND these rules. Dates also relative: @now, @now-2h
                        (s, m, h, d, w).{tab === JSON_TAB
                        ? ' A list of rules [left, operator, right], groups as {"or": [...]}. Empty = no extra rules.'
                        : ' Fields: those of the class of the job\'s bucket. No conditions = no extra rules.'}
                    </Typography>
                    {note != null && <Alert severity='info'>{note}</Alert>}
                    {tab === BUILDER && builder != null &&
                        <Query
                            {...builder.config}
                            value={builder.tree}
                            onChange={tree => {
                                setBuilder(current => ({...current, tree}));
                                setError(null);
                            }}
                            renderBuilder={renderBuilder}
                        />}
                    {tab === JSON_TAB &&
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
                            InputProps={{sx: {fontFamily: 'monospace', fontSize: '0.85rem'}}}
                        />}
                    {error != null && <Alert severity='error'>{error}</Alert>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpen(false)} color='inherit'>Cancel</Button>
                    <Button onClick={save} variant='contained'>OK</Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
