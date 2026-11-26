import MaterialTable from '@material-table/core';
import React, {useCallback, useContext, useEffect, useState} from "react";
import {Autocomplete, Box, TextField, Tooltip, useTheme} from "@mui/material";
import {History as HistoryIcon, PlayArrow, PlaylistPlay, Refresh} from "@mui/icons-material";
import {getProjectTableKey} from "../../utils/ConfigurationStorage";
import useTableState from "../../utils/TableState";
import {selectCellText} from "../../utils/TableCellSelect";
import {
    getDeleteOptions,
    getGetOptions,
    getPageSizeOptions,
    getPostOptions,
    getPutOptions,
    getSettingsTableHeight,
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {isItemChanged, validateItem} from "../../utils/JsonHelper";
import {MessageBox} from "../utils/MessageBox";
import {
    getColumnDescription,
    getColumnEnabled,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName
} from "../utils/StandardColumns";
import {getBaseUrl} from "../../utils/UrlBuilder";
import BucketsContext from "../../context/buckets/BucketsContext";
import TasksContext from "../../context/tasks/TasksContext";
import ScheduleRunsDialog, {StatusIcon} from "../dialogs/ScheduleRunsDialog";
import RunKeywordDialog from "../dialogs/RunKeywordDialog";
import EditJobRulesDialog, {RULES_PLACEHOLDER} from "../dialogs/EditJobRulesDialog";
import ClassesContext from "../../context/classes/ClassesContext";
import TagsContext from "../../context/tags/TagsContext";
import UsersContext from "../../context/users/UsersContext";
import EnumsContext from "../../context/enums/EnumsContext";
import {getBucketTags} from "../data/BucketDataTableHelper";
import {getClassById} from "../../utils/JsonHelper";
import TableContainer from "../utils/TableContainer";

const browserTimeZone = () => {
    try {
        return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    } catch (e) {
        return 'UTC';
    }
};

const formatDate = value => value != null ? new Date(value).toLocaleString() : '';

// the rules of a row as text (a list; the dialog has already checked them)
const rulesText = rules => rules == null ? '' : typeof rules === 'string' ? rules : JSON.stringify(rules);

// the keywords of the jobs, each once (ignoring case), sorted
const keywordsOf = jobs => {
    const byLower = new Map();
    (jobs || []).forEach(job => {
        if (job.keyword && !byLower.has(job.keyword.toLowerCase()))
            byLower.set(job.keyword.toLowerCase(), job.keyword);
    });
    return [...byLower.values()].sort((a, b) => a.localeCompare(b));
};

/**
 * Jobs: a saved task run on a bucket by cron (as the admin who saved the job: Cron runs as), by its keyword (all the
 * jobs of the keyword, as the caller) or by Run (as the caller). No cron and no keyword: only Run. Saving checks the
 * job as it will run (the task of the bucket, the rules, the access) on the server.
 */
export default function JobsTab() {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [schedules, setSchedules] = useState(null);
    const [historyOf, setHistoryOf] = useState(null);
    const [keywordOpen, setKeywordOpen] = useState(false);
    const tableState = useTableState(getProjectTableKey('settings-schedules'));
    const {buckets, fetchBuckets} = useContext(BucketsContext);
    const {tasks, fetchTasks} = useContext(TasksContext);
    const {classes, fetchClasses} = useContext(ClassesContext);
    const {tags, fetchTags} = useContext(TagsContext);
    const {users, fetchUsers} = useContext(UsersContext);
    const {enums, fetchEnums} = useContext(EnumsContext);
    const changeableFields = ['name', 'description', 'taskId', 'bucketId', 'rules', 'properties', 'cron', 'keyword', 'timeZone', 'enabled'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    const showError = error => setMessageBox({open: true, severity: 'error', title: 'Error', message: error});

    const fetchSchedules = useCallback(() => {
        fetch(getBaseUrl('task-schedules'), getGetOptions())
            .then(handleErrors)
            .then(setSchedules)
            .catch(showError);
    }, []);

    useEffect(() => {
        if (buckets == null)
            fetchBuckets();
    }, [buckets, fetchBuckets]);

    useEffect(() => {
        if (tasks == null)
            fetchTasks();
    }, [tasks, fetchTasks]);

    // for the rules builder
    useEffect(() => {
        if (classes == null)
            fetchClasses();
    }, [classes, fetchClasses]);

    useEffect(() => {
        if (tags == null)
            fetchTags();
    }, [tags, fetchTags]);

    useEffect(() => {
        if (users == null)
            fetchUsers();
    }, [users, fetchUsers]);

    useEffect(() => {
        if (enums == null)
            fetchEnums();
    }, [enums, fetchEnums]);

    // the fields of the rules builder: the class of the bucket chosen in the edited row, its tags
    const builderProps = bucketId => {
        const bucket = (buckets || []).find(item => item.id === Number(bucketId));
        const dataClass = bucket != null ? getClassById(classes, bucket.classId) : null;
        return {
            classProperties: dataClass?.configuration || [],
            tags: bucket != null ? getBucketTags(bucket, tags || []) : [],
            users: users || [],
            enums: enums || []
        };
    };

    useEffect(() => {
        fetchSchedules();
    }, [fetchSchedules]);

    const lookup = items => items != null
        ? Object.fromEntries(items.map(item => [item.id, item.name]))
        : {};

    // the row as sent to the server: the rules from their text
    const toPayload = row => {
        const text = rulesText(row.rules).trim();
        let rules = null;
        if (text !== '') {
            rules = JSON.parse(text); // a SyntaxError is shown by the caller
            if (!Array.isArray(rules))
                throw new Error('The rules are a list: ' + RULES_PLACEHOLDER);
        }
        return {
            id: row.id, name: row.name, description: row.description,
            taskId: row.taskId != null ? Number(row.taskId) : null,
            bucketId: row.bucketId != null ? Number(row.bucketId) : null,
            rules, properties: row.properties || null, cron: row.cron, keyword: row.keyword, timeZone: row.timeZone, enabled: row.enabled === true
        };
    };

    const replace = schedule => setSchedules(current => current.map(item => item.id === schedule.id ? schedule : item));

    const save = (row, create) => new Promise((resolve, reject) => {
        const message = validateItem(row, fieldsSpecification);
        if (message != null) {
            setMessageBox({open: true, severity: 'warning', title: 'Item is not valid', message});
            reject();
            return;
        }
        let payload;
        try {
            payload = toPayload(row);
        } catch (e) {
            setMessageBox({open: true, severity: 'warning', title: 'The rules are not valid JSON', message: e.message});
            reject();
            return;
        }
        fetch(getBaseUrl('task-schedules'), create ? getPostOptions(payload) : getPutOptions(payload))
            .then(handleErrors)
            .then(schedule => {
                if (create)
                    setSchedules(current => [...(current || []), schedule]);
                else
                    replace(schedule);
                resolve();
            })
            .catch(error => {
                showError(error);
                reject();
            });
    });

    const runNow = row => {
        fetch(getBaseUrl(`task-schedules/${row.id}/run`), getPostOptions({}))
            .then(handleErrors)
            .then(schedule => {
                replace(schedule);
                setMessageBox({
                    open: true,
                    severity: schedule.lastStatus === 'OK' ? 'success' : 'error',
                    title: schedule.lastStatus === 'OK' ? `"${schedule.name}" has run` : `"${schedule.name}" failed`,
                    message: schedule.lastMessage
                });
            })
            .catch(showError);
    };

    return (
        <div onDoubleClick={selectCellText}>
            <MaterialTable
                title='Jobs'
                tableRef={tableRef}
                columns={tableState.columns([
                    getColumnName(),
                    getColumnDescription(),
                    {title: 'Task', field: 'taskId', lookup: lookup(tasks)},
                    {title: 'Bucket', field: 'bucketId', lookup: lookup(buckets)},
                    {
                        title: 'Rules', field: 'rules', sorting: false, filtering: false,
                        // the number of conditions, the rules in the tooltip; edited in a dialog
                        render: rowData => <EditJobRulesDialog rules={rowData.rules}/>,
                        editComponent: props => (
                            <EditJobRulesDialog
                                rules={props.value}
                                properties={props.rowData.properties}
                                onChange={({rules, properties}) =>
                                    props.onRowDataChange({...props.rowData, rules, properties})}
                                {...builderProps(props.rowData.bucketId)}/>
                        )

                    },
                    {
                        title: 'Cron', field: 'cron', filtering: false,
                        editComponent: props => (
                            <TextField variant='standard' placeholder='0 2 * * *' value={props.value || ''}
                                       helperText='minute hour day month weekday; empty = no cron'
                                       onChange={e => props.onChange(e.target.value)}
                                       InputProps={{style: {fontFamily: 'monospace'}}}/>
                        )
                    },
                    {
                        title: 'Keyword', field: 'keyword',
                        editComponent: props => (
                            <Autocomplete
                                freeSolo
                                options={keywordsOf(schedules)}
                                inputValue={props.value || ''}
                                onInputChange={(event, value) => props.onChange(value)}
                                renderInput={params => <TextField {...params} variant='standard'
                                                                  placeholder='DEV cleaning'
                                                                  sx={{minWidth: 160}}/>}
                            />
                        )
                    },
                    {title: 'Time zone', field: 'timeZone', filtering: false, initialEditValue: browserTimeZone()},
                    {...getColumnEnabled(), initialEditValue: true},
                    {title: 'Cron runs as', field: 'runAs', editable: 'never'},
                    {
                        title: 'Next run', field: 'nextRunAt', editable: 'never', filtering: false,
                        render: rowData => formatDate(rowData.nextRunAt)
                    },
                    {
                        title: 'Last run', field: 'lastRunAt', editable: 'never', filtering: false,
                        render: rowData => rowData.lastRunAt == null ? '' : (
                            <Tooltip title={rowData.lastMessage || ''}>
                                <Box sx={{display: 'flex', alignItems: 'center', gap: 0.5}}>
                                    <StatusIcon status={rowData.lastStatus}/>
                                    {formatDate(rowData.lastRunAt)}
                                    {rowData.lastCount != null ? ` (${rowData.lastCount})` : ''}
                                </Box>
                            </Tooltip>
                        )
                    },
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ])}
                data={tableState.data(schedules != null ? schedules : [])}
                key={tableState.key}
                {...tableState.callbacks}
                options={{
                    ...tableState.options,
                    pageSizeOptions: getPageSizeOptions(),
                    paginationType: 'stepped',
                    actionsColumnIndex: -1,
                    maxColumnSort: 1,
                    search: true,
                    filtering: tableState.filtering,
                    debounceInterval: 700,
                    padding: 'dense',
                    headerStyle: {
                        position: 'sticky',
                        top: 0,
                        backgroundColor: getTableHeaderBackgroundColor(theme),
                        zIndex: 2
                    },
                    cellStyle: {whiteSpace: 'nowrap'},
                    maxBodyHeight: getSettingsTableHeight(),
                    minBodyHeight: getSettingsTableHeight(),
                    rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                }}
                components={{
                    Container: TableContainer
                }}
                actions={[
                    {
                        icon: () => <Refresh/>,
                        tooltip: 'Refresh',
                        isFreeAction: true,
                        onClick: () => fetchSchedules()
                    },
                    {
                        icon: tableState.filterIcon,
                        tooltip: 'Enable/disable filter',
                        isFreeAction: true,
                        onClick: tableState.toggleFiltering
                    },
                    {
                        icon: () => <PlaylistPlay/>,
                        tooltip: 'Run keyword',
                        isFreeAction: true,
                        onClick: () => setKeywordOpen(true)
                    },
                    {
                        icon: () => <PlayArrow/>,
                        tooltip: 'Run',
                        onClick: (event, rowData) => runNow(rowData)
                    },
                    {
                        icon: () => <HistoryIcon/>,
                        tooltip: 'Executions',
                        onClick: (event, rowData) => setHistoryOf(rowData)
                    }
                ]}
                editable={{
                    onRowAdd: newData => save(newData, true),
                    onRowUpdate: (newData, oldData) => {
                        if (!isItemChanged(oldData, newData, changeableFields)) {
                            setMessageBox({open: true, severity: 'info', title: 'Nothing changed', message: ''});
                            return Promise.reject();
                        }
                        return save(newData, false);
                    },
                    onRowDelete: oldData =>
                        fetch(getBaseUrl(`task-schedules/${oldData.id}`), getDeleteOptions())
                            .then(handleErrors)
                            .then(() => setSchedules(current => current.filter(item => item.id !== oldData.id)))
                            .catch(showError)
                }}
            />
            <ScheduleRunsDialog schedule={historyOf} onClose={() => setHistoryOf(null)}/>
            <RunKeywordDialog open={keywordOpen} keywords={keywordsOf(schedules)}
                              onClose={() => setKeywordOpen(false)} onRun={fetchSchedules}/>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}
