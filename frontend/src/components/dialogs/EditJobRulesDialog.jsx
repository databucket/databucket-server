import React, {useEffect, useRef, useState} from 'react';
import PropTypes from 'prop-types';
import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    Tab,
    Tabs,
    Tooltip,
    Typography,
    useTheme
} from '@mui/material';
import {MoreHoriz} from '@mui/icons-material';
import {Query, Utils as QbUtils} from '@react-awesome-query-builder/mui';
import {JsonEditor as Editor} from 'jsoneditor-react18';
import ace from 'brace';
import 'brace/mode/json';
import 'brace/theme/monokai';
import {createConfig, getInitialTree, renderBuilder} from '../utils/QueryBuilderHelper';
import PropertiesTable from '../utils/PropertiesTable';
import {logicToRules} from '../../utils/LogicToRules';
import {rulesToLogic, sameLogic} from '../../utils/RulesToLogic';
import {getJobRulesDialogSize, setJobRulesDialogSize} from '../../utils/ConfigurationStorage';

export const RULES_PLACEHOLDER = '[["modifiedAt", "<", "@now-1d"]]';

const RULES = 0;
const PROPERTIES = 1;
const JSON_TAB = 2;

const NOT_IN_BUILDER = 'These rules can\'t be shown in the Rules tab (e.g. an operator like similar or ~, a property'
    + ' missing in Properties, a value written differently than the builder writes it): edit them as JSON.';

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
        return {error: 'A condition can\'t be saved as rules (e.g. starts with, not any in): change it or edit the'
                + ' rules as JSON.'};
    return {rules: rules.length > 0 ? rules : null};
};

// the job's own properties over those of the class (by path), as the Properties tab shows them
const mergeWithClass = (own, classProperties) => {
    const list = [...(own || [])];
    (classProperties || []).forEach(property => {
        if (!list.some(item => item.path === property.path))
            list.push(property);
    });
    return list;
};

// what the job keeps: the properties that are not in the class as they are there (null when none)
const ownProperties = (properties, classProperties) => {
    const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);
    const own = properties.filter(property => {
        const inClass = (classProperties || []).find(item => item.path === property.path);
        return inClass == null || !same(inClass, property);
    });
    return own.length > 0 ? own : null;
};

const jsonThemeLight = null;
const jsonThemeDark = 'ace/theme/monokai';

EditJobRulesDialog.propTypes = {
    // a list, or text while not yet valid
    rules: PropTypes.oneOfType([PropTypes.array, PropTypes.string]),
    // the job's own properties of the builder (besides those of the class)
    properties: PropTypes.array,
    // with it the button opens the dialog: called with {rules, properties}
    onChange: PropTypes.func,
    // for the builder: the properties of the class of the job's bucket, its tags, the users, the enums
    classProperties: PropTypes.array,
    tags: PropTypes.array,
    users: PropTypes.array,
    enums: PropTypes.array
};

/**
 * The extra rules of a job (Settings > Jobs): in the table the number of conditions, the rules in the tooltip; with
 * onChange (row edited) the button opens a dialog with the rules in the query builder (Rules: the properties of the
 * bucket's class and the job's own, Properties) or as JSON; saved as the list of rules either way. Rules the builder
 * can't show open as JSON.
 */
export default function EditJobRulesDialog(props) {
    const theme = useTheme();
    const dialogContentRef = useRef(null);
    const jsonEditorRef = useRef(null);
    const [open, setOpen] = useState(false);
    const [size, setSize] = useState(getJobRulesDialogSize());
    const [tab, setTab] = useState(RULES);
    const [text, setText] = useState('');
    const [properties, setProperties] = useState([]);
    const [builder, setBuilder] = useState(null); // {config, tree}
    const [error, setError] = useState(null);
    const [note, setNote] = useState(null);
    const count = countRules(props.rules);
    const formatted = formatRules(props.rules);

    const makeConfig = propertyList => createConfig(propertyList, props.tags || [], props.users || [],
        props.enums || [], theme);

    // the JSON editor shows the text whenever the tab opens
    useEffect(() => {
        if (open && tab === JSON_TAB && jsonEditorRef.current?.jsonEditor != null)
            jsonEditorRef.current.jsonEditor.setText(text);
    }, [open, tab]);

    const editorText = () => jsonEditorRef.current?.jsonEditor != null
        ? jsonEditorRef.current.jsonEditor.getText()
        : text;

    const openDialog = () => {
        const merged = mergeWithClass(props.properties, props.classProperties);
        const config = makeConfig(merged);
        const list = asList(props.rules);
        const tree = typeof list === 'string' ? null : treeOf(list, config);
        setProperties(merged);
        setText(formatted);
        setError(null);
        setBuilder({config, tree: tree || treeOf(null, config)});
        setNote(tree == null ? NOT_IN_BUILDER : null);
        setTab(tree == null ? JSON_TAB : RULES);
        setOpen(true);
    };

    // leaving a tab: its content into the state, or an error (stays on the tab)
    const leave = () => {
        if (tab === JSON_TAB) {
            const current = editorText();
            const result = parseRules(current);
            if (result.error != null)
                return result.error;
            setText(current);
            return null;
        }
        if (tab === RULES) {
            const result = rulesOfTree(builder.tree, builder.config);
            if (result.error != null)
                return result.error;
            setText(formatRules(result.rules));
        }
        return null;
    };

    const changeTab = (event, newTab) => {
        if (newTab === tab)
            return;
        const leaveError = leave();
        if (leaveError != null) {
            setError(leaveError);
            return;
        }
        if (newTab === RULES) {
            // the rules of the JSON tab (or as the Rules tab left them) with the properties as they are now
            const config = makeConfig(properties);
            const source = parseRules(tab === JSON_TAB ? editorText() : text).rules;
            const tree = treeOf(source, config);
            if (tree == null) {
                setError(NOT_IN_BUILDER);
                return;
            }
            setBuilder({config, tree});
        }
        setError(null);
        setNote(null);
        setTab(newTab);
    };

    const apply = () => {
        const result = tab === JSON_TAB ? parseRules(editorText())
            : tab === RULES ? rulesOfTree(builder.tree, builder.config)
                : parseRules(text);
        if (result.error != null) {
            setError(result.error);
            return;
        }
        props.onChange({rules: result.rules, properties: ownProperties(properties, props.classProperties)});
        setOpen(false);
    };

    const changeSize = newSize => {
        setSize(newSize);
        setJobRulesDialogSize(newSize);
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
            <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth={size}>
                <DialogTitle sx={{display: 'flex', alignItems: 'center', position: 'relative'}}>
                    <Box sx={{flex: 1}}>Job rules</Box>
                    <Tabs value={tab} onChange={changeTab} textColor='secondary' indicatorColor='secondary'
                          sx={{position: 'absolute', left: '50%', transform: 'translateX(-50%)'}}>
                        <Tab label='Rules'/>
                        <Tab label='Properties'/>
                        <Tab label='JSON'/>
                    </Tabs>
                    <Tooltip title='Smaller'>
                        <span>
                            <IconButton aria-label='Smaller' color='inherit' disabled={size !== 'lg'}
                                        onClick={() => changeSize('md')}>
                                <span className='material-icons'>fullscreen_exit</span>
                            </IconButton>
                        </span>
                    </Tooltip>
                    <Tooltip title='Larger'>
                        <span>
                            <IconButton aria-label='Larger' color='inherit' disabled={size !== 'md'}
                                        onClick={() => changeSize('lg')}>
                                <span className='material-icons'>fullscreen</span>
                            </IconButton>
                        </span>
                    </Tooltip>
                </DialogTitle>
                <DialogContent dividers ref={dialogContentRef}
                               sx={{p: 0, height: '62vh', display: 'flex', flexDirection: 'column'}}>
                    {tab !== PROPERTIES &&
                        <Typography variant='body2' color='text.secondary' sx={{px: 2, pt: 1}}>
                            {tab === RULES && 'The rows of the task filter AND these rules. Dates also relative:'
                                + ' @now, @now-2h (s, m, h, d, w). No conditions = no extra rules.'}
                            {tab === JSON_TAB && 'The rows of the task filter AND these rules: a list of rules'
                                + ' [left, operator, right], groups as {"or": [...]}; dates also relative: @now-2h.'
                                + ' Empty = no extra rules.'}
                        </Typography>}
                    {note != null && <Alert severity='info' sx={{mx: 2, mt: 1}}>{note}</Alert>}
                    {error != null && <Alert severity='error' sx={{mx: 2, mt: 1}}>{error}</Alert>}
                    {tab === RULES && builder != null &&
                        <Query
                            {...builder.config}
                            value={builder.tree}
                            onChange={tree => {
                                setBuilder(current => ({...current, tree}));
                                setError(null);
                            }}
                            renderBuilder={renderBuilder}
                        />}
                    {tab === PROPERTIES &&
                        <PropertiesTable
                            data={properties}
                            enums={props.enums || []}
                            onChange={setProperties}
                            title='Class origin and defined properties:'
                            pageSize={null}
                            parentContentRef={dialogContentRef}
                        />}
                    {tab === JSON_TAB &&
                        <Box sx={{flex: 1, minHeight: 0, mt: 1}}>
                            <Editor
                                ref={jsonEditorRef}
                                value={[]}
                                mode='code'
                                ace={ace}
                                theme={theme.palette.mode === 'light' ? jsonThemeLight : jsonThemeDark}
                                statusBar={false}
                                htmlElementProps={{style: {height: '100%'}}}
                            />
                        </Box>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpen(false)} color='inherit'>Cancel</Button>
                    <Button onClick={apply} variant='contained'>Apply</Button>
                </DialogActions>
            </Dialog>
        </>
    );
}
