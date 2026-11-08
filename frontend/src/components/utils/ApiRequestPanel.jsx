import React, {useEffect, useMemo, useRef, useState} from "react";
import PropTypes from "prop-types";
import {Box, Button, Tab, Tabs, Tooltip, Typography, useTheme} from "@mui/material";
import {ContentCopy, WarningAmber} from "@mui/icons-material";
import {JsonEditor as Editor} from 'jsoneditor-react18';
import ace from 'brace';
import 'brace/mode/json';
import "brace/theme/monokai";
import {logicToRules} from "../../utils/LogicToRules";
import {getDataUrl} from "../../utils/UrlBuilder";
import {getPostOptions} from "../../utils/MaterialTableHelper";
import {API_VERSION_HEADER, handleErrors} from "../../utils/FetchHelper";

// the tab shown under the rules (SQL or API request) is remembered in the browser
const TAB_KEY = 'rich-filter-preview-tab';
const readTab = () => {
    try {
        return localStorage.getItem(TAB_KEY) === 'api' ? 'api' : 'sql';
    } catch (e) {
        return 'sql';
    }
};
const saveTab = (tab) => {
    try {
        localStorage.setItem(TAB_KEY, tab);
    } catch (e) {
        // not remembered
    }
};

const HEADERS = {
    'Authorization': 'Bearer $TOKEN',
    'Content-Type': 'application/json',
    ...API_VERSION_HEADER
};

// a value on one line, with a space after the commas: ["$.a", "in", [1, 2]]
const inline = (value) => {
    if (Array.isArray(value))
        return '[' + value.map(inline).join(', ') + ']';
    if (value != null && typeof value === 'object')
        return '{' + Object.entries(value).map(([key, v]) => JSON.stringify(key) + ': ' + inline(v)).join(', ') + '}';
    return JSON.stringify(value);
};

const isRule = (item) => Array.isArray(item) && item.length === 3 && typeof item[1] === 'string';

// the rules as in the wiki: a rule per line, the groups ({"or": [...]}, {"!": ...}) indented
const formatItem = (item, indent) => {
    if (isRule(item) || item == null || typeof item !== 'object')
        return inline(item);
    const inner = indent + '  ';
    if (Array.isArray(item))
        return '[\n' + item.map(i => inner + formatItem(i, inner)).join(',\n') + '\n' + indent + ']';
    const [operator, value] = Object.entries(item)[0];
    return '{' + JSON.stringify(operator) + ': ' + formatItem(value, indent) + '}';
};

const formatBody = (body) => body.rules != null
    ? (body.rules.length === 0 ? '{\n  "rules": []\n}' : '{\n  "rules": ' + formatItem(body.rules, '  ') + '\n}')
    : JSON.stringify(body, null, 2);

const count = (url, body) => fetch(url, getPostOptions(body)).then(handleErrors).then(result => result.total);

/**
 * Under the rules of the rich filter, two tabs: the filter as an SQL condition, what follows WHERE (given as `sql`), and as the body of an API request,
 * to copy into an API client (Postman, Bruno) or a test. The request holds the "rules" format of the API; when the
 * logic can't be expressed by rules, the logic itself.
 */
export default function ApiRequestPanel({bucket, logic, sql}) {
    const theme = useTheme();
    const [tab, setTab] = useState(readTab);
    const open = tab === 'api';
    const [copied, setCopied] = useState(null);
    const [check, setCheck] = useState(null);   // {rules, logic}: the numbers of rows both match
    const editorRef = useRef(null);

    const rules = useMemo(() => logicToRules(logic), [logic]);
    const body = rules != null ? {rules} : {logic};
    const text = formatBody(body);
    const url = `${getDataUrl(bucket)}/get?limit=10`;
    const curl = `curl -X POST "${url}" \\\n`
        + Object.entries(HEADERS).map(([name, value]) => `  -H "${name}: ${value}" \\\n`).join('')
        + `  -d '${JSON.stringify(body).replaceAll("'", "'\\''")}'`;   // an apostrophe in a value closes and reopens the quotes

    // the rules and the logic must match the same rows; checked while the panel is open
    useEffect(() => {
        setCheck(null);
        if (!open || rules == null || rules.length === 0)
            return;
        const timer = setTimeout(() => {
            const countUrl = `${getDataUrl(bucket)}/get?limit=0`;
            Promise.all([count(countUrl, {rules}), count(countUrl, {logic})])
                .then(([byRules, byLogic]) => setCheck({rules: byRules, logic: byLogic}))
                .catch(() => setCheck(null));
        }, 1000);
        return () => clearTimeout(timer);
    }, [open, text]);

    useEffect(() => {
        editorRef.current?.jsonEditor?.setText(text);
    }, [text, open, theme.palette.mode]);

    const changeTab = (event, value) => {
        saveTab(value);
        setTab(value);
    };

    const copy = async (what, value) => {
        try {
            await navigator.clipboard.writeText(value);
            setCopied(what);
            setTimeout(() => setCopied(null), 1500);
        } catch (e) {
            setCopied('error');
        }
    };

    const lines = text.split('\n').length;
    const differs = check != null && check.rules !== check.logic;

    return (
        <Box sx={{mx: 2, mb: 2}}>
            <Box display="flex" alignItems="center" sx={{borderBottom: 1, borderColor: 'divider'}}>
                <Tabs value={tab} onChange={changeTab} textColor="secondary" indicatorColor="secondary"
                      sx={{minHeight: 36, '& .MuiTab-root': {minHeight: 36, py: 0.5}}}>
                    <Tab value="sql" label="SQL condition"/>
                    <Tab value="api" label="API request"/>
                </Tabs>
                <Box flexGrow={1}/>
                {open && <>
                    <Tooltip title={copied === 'json' ? 'Copied' : 'Copy the JSON body'}>
                        <Button size="small" onClick={() => copy('json', text)} startIcon={<ContentCopy/>}>JSON</Button>
                    </Tooltip>
                    <Tooltip title={copied === 'curl' ? 'Copied' : 'Copy the request as curl'}>
                        <Button size="small" onClick={() => copy('curl', curl)} startIcon={<ContentCopy/>}>curl</Button>
                    </Tooltip>
                </>}
            </Box>
            {!open && <Box sx={{mx: -2}}>{sql}</Box>}
            {open && <>
                <Typography variant="body2" component="div" sx={{fontFamily: 'monospace', my: 1, wordBreak: 'break-all'}}>
                    POST {url}
                    {Object.entries(HEADERS).map(([name, value]) =>
                        <div key={name} style={{opacity: 0.75}}>{name}: {value}</div>)}
                </Typography>
                <Box sx={{height: Math.min(lines * 17 + 12, 320)}}>
                    <Editor
                        key={theme.palette.mode}
                        ref={editorRef}
                        value={{}}
                        mode="code"
                        ace={ace}
                        theme={theme.palette.mode === 'light' ? null : "ace/theme/monokai"}
                        onEditable={() => false}
                        mainMenuBar={false}
                        statusBar={false}
                        htmlElementProps={{style: {height: "100%"}}}
                    />
                </Box>
                {rules == null &&
                    <Typography variant="caption" color="text.secondary">
                        This filter can't be written as rules: the body holds its logic (JsonLogic), which the API takes too.
                    </Typography>}
                {differs &&
                    <Typography variant="caption" color="error" display="flex" alignItems="center" gap={0.5}>
                        <WarningAmber fontSize="inherit"/>
                        The rules match {check.rules} rows, the filter {check.logic}. Please report it; use the logic of the filter meanwhile.
                    </Typography>}
                {copied === 'error' &&
                    <Typography variant="caption" color="error">The browser doesn't allow copying here: select the text and copy it.</Typography>}
            </>}
        </Box>
    );
}

ApiRequestPanel.propTypes = {
    bucket: PropTypes.object.isRequired,
    logic: PropTypes.object,
    sql: PropTypes.node
};
