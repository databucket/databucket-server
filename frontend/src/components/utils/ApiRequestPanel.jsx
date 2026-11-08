import React, {useEffect, useMemo, useRef, useState} from "react";
import PropTypes from "prop-types";
import {Box, Button, Tooltip, Typography, useTheme} from "@mui/material";
import {ContentCopy, WarningAmber} from "@mui/icons-material";
import {JsonEditor as Editor} from 'jsoneditor-react18';
import ace from 'brace';
import 'brace/mode/json';
import "brace/theme/monokai";
import {logicToRules} from "../../utils/LogicToRules";
import {getDataUrl} from "../../utils/UrlBuilder";
import {getPostOptions} from "../../utils/MaterialTableHelper";
import {API_VERSION_HEADER, handleErrors} from "../../utils/FetchHelper";

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

// the body: the rules as above, the other items (logic, the changes of a task...) as JSON indented by 2
const formatBody = (body) => '{\n' + Object.entries(body).map(([key, value]) => {
    const formatted = key === 'rules'
        ? (value.length === 0 ? '[]' : formatItem(value, '  '))
        : JSON.stringify(value, null, 2).replaceAll('\n', '\n  ');
    return '  ' + JSON.stringify(key) + ': ' + formatted;
}).join(',\n') + '\n}';

const count = (url, body) => fetch(url, getPostOptions(body)).then(handleErrors).then(result => result.total);

/**
 * The rules (rich filter, task execution) as an API request, to copy into an API client (Postman, Bruno) or a test:
 * the method, the URL, the headers and the body. By default the request reads the matching data; a task gives its
 * own `method`, `url` and the other items of the body (`extra`). The body holds the "rules" format of the API; when
 * the logic can't be expressed by rules, the logic itself.
 */
export default function ApiRequestPanel({bucket, logic, method = 'POST', url: requestUrl, extra = {}}) {
    const theme = useTheme();
    const [copied, setCopied] = useState(null);
    const [check, setCheck] = useState(null);   // {rules, logic}: the numbers of rows both match
    const editorRef = useRef(null);

    const rules = useMemo(() => logicToRules(logic), [logic]);
    const body = {...(rules != null ? {rules} : {logic}), ...extra};
    const text = formatBody(body);
    const url = requestUrl ?? `${getDataUrl(bucket)}/get?limit=10`;
    const curl = `curl -X ${method} "${url}" \\\n`
        + Object.entries(HEADERS).map(([name, value]) => `  -H "${name}: ${value}" \\\n`).join('')
        + `  -d '${JSON.stringify(body).replaceAll("'", "'\\''")}'`;   // an apostrophe in a value closes and reopens the quotes

    // the rules and the logic must match the same rows; checked while the request is shown
    useEffect(() => {
        setCheck(null);
        if (rules == null || rules.length === 0)
            return;
        const timer = setTimeout(() => {
            const countUrl = `${getDataUrl(bucket)}/get?limit=0`;
            Promise.all([count(countUrl, {rules}), count(countUrl, {logic})])
                .then(([byRules, byLogic]) => setCheck({rules: byRules, logic: byLogic}))
                .catch(() => setCheck(null));
        }, 1000);
        return () => clearTimeout(timer);
    }, [text]);

    // the line height of the editor (it depends on the zoom and the fonts); read once the editor is shown
    const [lineHeight, setLineHeight] = useState(19);

    useEffect(() => {
        const jsonEditor = editorRef.current?.jsonEditor;
        jsonEditor?.setText(text);
        const measured = jsonEditor?.aceEditor?.renderer?.lineHeight;
        if (measured > 0 && measured !== lineHeight)
            setLineHeight(measured);
        jsonEditor?.aceEditor?.resize();
    }, [text, theme.palette.mode, lineHeight]);

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
        <Box sx={{m: 2}}>
            <Box display="flex" alignItems="flex-start" gap={2}>
                <Typography variant="body2" component="div" sx={{fontFamily: 'monospace', wordBreak: 'break-all', flexGrow: 1}}>
                    {method} {url}
                    {Object.entries(HEADERS).map(([name, value]) =>
                        <div key={name} style={{opacity: 0.75}}>{name}: {value}</div>)}
                </Typography>
                <Box display="flex" flexShrink={0}>
                    <Tooltip title={copied === 'json' ? 'Copied' : 'Copy the JSON body'}>
                        <Button size="small" color="inherit" onClick={() => copy('json', text)} startIcon={<ContentCopy/>}>JSON</Button>
                    </Tooltip>
                    <Tooltip title={copied === 'curl' ? 'Copied' : 'Copy the request as curl'}>
                        <Button size="small" color="inherit" onClick={() => copy('curl', curl)} startIcon={<ContentCopy/>}>curl</Button>
                    </Tooltip>
                </Box>
            </Box>
            {/* the whole body without a scrollbar of its own, and a spare line (the dialog scrolls if needed) */}
            <Box sx={{height: (lines + 1) * lineHeight + 12, mt: 1}}>
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
                    These rules can't be written in the rules format: the body holds their logic (JsonLogic), which the API takes too.
                </Typography>}
            {differs &&
                <Typography variant="caption" color="error" display="flex" alignItems="center" gap={0.5}>
                    <WarningAmber fontSize="inherit"/>
                    The rules match {check.rules} rows, the logic {check.logic}. Please report it; use the logic meanwhile.
                </Typography>}
            {copied === 'error' &&
                <Typography variant="caption" color="error">The browser doesn't allow copying here: select the text and copy it.</Typography>}
        </Box>
    );
}

ApiRequestPanel.propTypes = {
    bucket: PropTypes.object.isRequired,
    logic: PropTypes.object,
    method: PropTypes.string,
    url: PropTypes.string,
    extra: PropTypes.object
};
