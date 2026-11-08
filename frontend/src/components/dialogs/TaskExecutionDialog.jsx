import React, {useCallback, useContext, useEffect, useState} from 'react';
import {
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions as MuiDialogActions,
    DialogContent as MuiDialogContent,
    DialogTitle as MuiDialogTitle,
    Grid,
    IconButton,
    styled,
    Tab,
    Tabs,
    Typography,
    useTheme
} from '@mui/material';
import {Close as CloseIcon} from '@mui/icons-material';
import PropTypes from "prop-types";
import {
    getDeleteOptions,
    getPostOptions,
    getPutOptions
} from "../../utils/MaterialTableHelper";
import {MessageBox} from "../utils/MessageBox";
import TaskActions, {isReserveValid, reserveActions} from "../utils/TaskActions";
import PropertiesTable from "../utils/PropertiesTable";
import {getBucketTags, getBucketTasks} from "../data/BucketDataTableHelper";
import EnumsProvider from "../../context/enums/EnumsProvider";
import TaskMenuSelector from "../data/TaskMenuSelector";
import {handleErrors} from "../../utils/FetchHelper";
import {
    getClearDataHistoryByRulesUrl,
    getDataReserveUrl,
    getDataUrl
} from "../../utils/UrlBuilder";
import {getClassById, getPropertyByUuid} from "../../utils/JsonHelper";
import {Query, Utils as QbUtils} from '@react-awesome-query-builder/mui';
import {
    createConfig,
    getInitialTree,
    renderBuilder,
    renderResult
} from "../utils/QueryBuilderHelper";
import AccessContext from "../../context/access/AccessContext";
import {
    getTaskExecutionDialogSize,
    getUsername,
    hasAdminRole,
    setTaskExecutionDialogSize
} from "../../utils/ConfigurationStorage";
import {debounce} from "../utils/Debouncer";
import ApiRequestPanel from "../utils/ApiRequestPanel";

const PREFIX = 'TaskExecutionDialog';

const classes = {
    root: `${PREFIX}-root`,
    root2: `${PREFIX}-root2`,
    root3: `${PREFIX}-root3`,
    selected: `${PREFIX}-selected`,
    dialogPaper: `${PREFIX}-dialogPaper`,
    oneLine: `${PREFIX}-oneLine`,
    tabs: `${PREFIX}-tabs`,
    devGrabSpace: `${PREFIX}-devGrabSpace`,
    divActionGrabSpace: `${PREFIX}-divActionGrabSpace`,
    container: `${PREFIX}-container`,
    closeButton: `${PREFIX}-closeButton`,
    smallerButton: `${PREFIX}-smallerButton`,
    largerButton: `${PREFIX}-largerButton`
};

const StyledDialog = styled(Dialog)(({theme}) => ({
    [`& .${classes.dialogPaper}`]: {
        minHeight: '80vh',
    },

    [`& .${classes.oneLine}`]: {
        display: 'flex',
        alignItems: 'center',
        padding: theme.spacing(2)
    },

    [`& .${classes.tabs}`]: {
        flexGrow: 1
    },

    [`& .${classes.devGrabSpace}`]: {
        width: '250px'
    },

    [`& .${classes.divActionGrabSpace}`]: {
        width: '30px'
    },

    [`& .${classes.root}`]: {
        margin: 0,
        padding: theme.spacing(0),
    },
    [`& .${classes.container}`]: {
        display: 'flex',
        flexWrap: 'wrap',
    },
    [`& .${classes.closeButton}`]: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1)
    },
    [`& .${classes.smallerButton}`]: {
        position: 'absolute',
        right: theme.spacing(15),
        top: theme.spacing(1)
    },
    [`& .${classes.largerButton}`]: {
        position: 'absolute',
        right: theme.spacing(10),
        top: theme.spacing(1)
    }
}));

const DialogTitle = (props => {
    const {children, onClose, onMakeDialogSmaller, onMakeDialogLarger} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root}>
            <Typography variant="h6">{children}</Typography>
            <IconButton
                aria-label="Smaller"
                className={classes.smallerButton}
                onClick={onMakeDialogSmaller}
                color={"inherit"}
                disabled={onMakeDialogSmaller == null}
                size="large">
                <span className="material-icons">fullscreen_exit</span>
            </IconButton>
            <IconButton
                aria-label="Larger"
                className={classes.largerButton}
                onClick={onMakeDialogLarger}
                color={"inherit"}
                disabled={onMakeDialogLarger == null}
                size="large">
                <span className="material-icons">fullscreen</span>
            </IconButton>
            {onClose ? (
                <IconButton
                    aria-label="Close"
                    className={classes.closeButton}
                    onClick={onClose}
                    size="large">
                    <CloseIcon/>
                </IconButton>
            ) : null}
        </MuiDialogTitle>
    );
});

const DialogContent = MuiDialogContent;

const DialogActions = MuiDialogActions;

TaskExecutionDialog.propTypes = {
    open: PropTypes.bool.isRequired,
    bucket: PropTypes.object.isRequired,
    onClose: PropTypes.func.isRequired,
    reload: PropTypes.func.isRequired,
    activeLogic: PropTypes.object,
    // the features of the view: tasks (remove, modify, clear history, saved tasks) and reservation (reserve data)
    tasksEnabled: PropTypes.bool,
    reservationEnabled: PropTypes.bool
};

const initialActions = {properties: []};

export default function TaskExecutionDialog(props) {

    const theme = useTheme();
    const accessContext = useContext(AccessContext);
    const bucketTags = getBucketTags(props.bucket, accessContext.tags);
    const tasksEnabled = props.tasksEnabled !== false;
    const reservationEnabled = props.reservationEnabled === true;
    // without tasks the dialog only reserves data
    const startActions = () => tasksEnabled ? initialActions : reserveActions(getUsername());
    const [activeTab, setActiveTab] = useState(0);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [appliesCount, setAppliesCount] = useState(0);
    const [state, setState] = useState({
        actions: initialActions,
        properties: [],
        logic: null,
        tree: null,
        config: null,
        processing: false
    });
    const [dialogSize, setDialogSize] = useState('md');
    const dialogContentRef = React.useRef(null);

    useEffect(() => {
        setDialogSize(getTaskExecutionDialogSize());
    }, []);

    useEffect(() => {
        if (props.open) {
            setActiveTab(0);
            const properties = getClassProperties();
            const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums, theme);
            const tree = QbUtils.checkTree(getInitialTree(props.activeLogic, null, config), config);
            setState({
                ...state,
                properties: getClassProperties(),
                actions: startActions(),
                logic: props.activeLogic,
                tree: tree,
                config: config
            });
        }
    }, [props.open]);


    const onTaskSelected = (task) => {
        if (task.filterId != null) {
            const filter = accessContext.filters.filter(f => f.id === task.filterId)[0];
            const properties = getMergedProperties(getClassProperties(), task.configuration.properties, filter.configuration.properties);
            const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums, theme);
            const tree = QbUtils.checkTree(getInitialTree(filter.configuration.logic, filter.configuration.tree, config), config);
            setState({
                ...state,
                actions: task.configuration.actions,
                logic: filter.configuration.logic,
                tree: tree,
                properties: properties,
                config: config
            });
        } else {
            const properties = getMergedProperties(getClassProperties(), task.configuration.properties, []);
            const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums, theme);
            const tree = QbUtils.checkTree(getInitialTree(null, null, config), config);
            setState({
                ...state,
                actions: task.configuration.actions,
                logic: null,
                tree: tree,
                properties: properties,
                config: config
            });
        }
    }

    // reserving takes only free rows (the server adds it, the rules don't show it): then only those are counted, by
    // the reservation with limit 0, which counts all free rows also in a bucket with protected data (where /get
    // returns only the user's own rows)
    const freeOnly = state.actions.type === 'reserve';

    useEffect(() => {
        refreshAppliesCount({open: props.open, bucket: props.bucket, logic: state.logic, freeOnly});
    }, [props.open, state.logic, freeOnly]);

    const refreshAppliesCount = useCallback(
        debounce(({open, bucket, logic, freeOnly}) => {
            if (open) {
                let resultOk = true;
                const request = freeOnly
                    ? fetch(getDataReserveUrl(bucket, 0, false), getPostOptions({logic}))
                    : fetch(getDataUrl(bucket) + '/get?limit=0', getPostOptions({logic}));
                request
                    .then(handleErrors)
                    .catch(error => {
                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                        resultOk = false;
                    })
                    .then(result => {
                        if (resultOk)
                            setAppliesCount(freeOnly ? result.available : result.total);
                    });
            }
        }, 1000),
        []
    );

    // the changes of the "modify" action: what the request sets or removes besides the rules (empty: nothing defined)
    const getModifyChanges = () => {
        const changes = {};
        if (state.actions.setTag != null && state.actions.setTag === true)
            changes.tagId = state.actions.tagId;
        if (state.actions.setReserved != null && state.actions.setReserved === true)
            changes.reserved = state.actions.reserved;
        if (state.actions.properties != null && state.actions.properties.length > 0) {
            let propertiesToSet = {};
            let propertiesToRemove = [];
            state.actions.properties.forEach(property => {
                const propertyDef = getPropertyByUuid(state.properties, property.uuid);
                if (propertyDef == null)
                    return;
                if (['setValue', 'setNull'].includes(property.action))
                    propertiesToSet[propertyDef.path] = property.value;
                else
                    propertiesToRemove.push(propertyDef.path);
            });
            if (Object.keys(propertiesToSet).length)
                changes.propertiesToSet = propertiesToSet;
            if (propertiesToRemove.length > 0)
                changes.propertiesToRemove = propertiesToRemove;
        }
        return changes;
    }

    // the request of the chosen action, besides the rules: its method, URL and the other items of the body.
    // Used to execute the task and to show it as an API request, so both are always the same.
    const getActionRequest = () => {
        const {type, number, random, username} = state.actions;
        switch (type) {
            case 'reserve': {
                const targetOwnerUsername = username !== getUsername() ? username : null;
                return {
                    method: 'POST', url: getDataReserveUrl(props.bucket, number, random),
                    extra: targetOwnerUsername != null ? {targetOwnerUsername} : {}
                };
            }
            case 'remove':
                return {method: 'DELETE', url: getDataUrl(props.bucket), extra: {}};
            case 'clear history':
                return {method: 'DELETE', url: getClearDataHistoryByRulesUrl(props.bucket), extra: {}};
            case 'modify':
                return {method: 'PUT', url: getDataUrl(props.bucket), extra: getModifyChanges()};
            default:
                return {method: 'POST', url: `${getDataUrl(props.bucket)}/get?limit=10`, extra: {}};
        }
    }

    // reserves free data rows matching the rules, for the current user or, for an admin, the chosen target owner
    const onReserve = () => {
        const {number} = state.actions;
        const {url, extra} = getActionRequest();
        const payload = {targetOwnerUsername: null, ...extra, logic: state.logic};
        setState({...state, processing: true});
        fetch(url, getPostOptions(payload))
            .then(handleErrors)
            .then(result => {
                setState({...state, processing: false});
                if (result.reserved > 0) {
                    const rows = `${result.reserved} data ${result.reserved === 1 ? 'row' : 'rows'}`;
                    setMessageBox({
                        open: true,
                        severity: result.reserved < number ? 'warning' : 'success',
                        title: result.reserved < number ? `Reserved ${rows} of ${number}` : `Reserved ${rows}`,
                        message: `${result.available} free ${result.available === 1 ? 'row matches' : 'rows match'} the rules now.`
                    });
                } else
                    setMessageBox({open: true, severity: 'info', title: 'No free data row matches the rules', message: null});
                refreshAppliesCount({open: props.open, bucket: props.bucket, logic: state.logic, freeOnly: true});
                props.reload();
            })
            .catch(error => {
                setState({...state, processing: false});
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
            });
    }

    const onTaskExecute = () => {
        let resultOk = true;
        if (state.actions.type === 'reserve') {
            onReserve();
        } else if (state.actions.type === 'remove') {
            setState({...state, processing: true});
            fetch(getDataUrl(props.bucket), getDeleteOptions({logic: state.logic}))
                .then(handleErrors)
                .catch(error => {
                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                    resultOk = false;
                })
                .then(result => {
                    setState({...state, processing: false});
                    if (resultOk) {
                        setMessageBox({open: true, severity: 'success', title: result.message, message: null});
                        refreshAppliesCount({open: props.open, bucket: props.bucket, logic: state.logic, freeOnly});
                        props.reload();
                    }
                });
        } else if (state.actions.type === 'clear history') {
            setState({...state, processing: true});
            fetch(getClearDataHistoryByRulesUrl(props.bucket), getDeleteOptions({logic: state.logic}))
                .then(handleErrors)
                .catch(error => {
                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                    resultOk = false;
                })
                .then(result => {
                    setState({...state, processing: false});
                    if (resultOk) {
                        setMessageBox({open: true, severity: 'success', title: result.message, message: null});
                        props.reload();
                    }
                });
        } else {
            const changes = getModifyChanges();
            const payload = {logic: state.logic, ...changes};

            if (Object.keys(changes).length > 0) {
                setState({...state, processing: true});
                fetch(getDataUrl(props.bucket), getPutOptions(payload))
                    .then(handleErrors)
                    .catch(error => {
                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                        resultOk = false;
                    })
                    .then(result => {
                        setState({...state, processing: false});
                        if (resultOk) {
                            setMessageBox({open: true, severity: 'success', title: result.message, message: null});
                            refreshAppliesCount({open: props.open, bucket: props.bucket, logic: state.logic, freeOnly});
                            props.reload();
                        }
                    });
            } else
                setMessageBox({
                    open: true,
                    severity: 'info',
                    title: "No modifications has been defined!",
                    message: null
                });
        }
    }

    const handleChangedTab = (event, newActiveTab) => {
        setActiveTab(newActiveTab);
    }

    const handleClose = () => {
        props.onClose();
    };

    const getClassProperties = () => {
        if (props.bucket.classId != null) {
            const dataClass = getClassById(accessContext.classes, props.bucket.classId);
            return dataClass.configuration || [];
        } else
            return [];
    }

    const getMergedProperties = (classProperties, taskProperties, filterProperties) => {
        let mergedProperties = [];

        classProperties.forEach(property => {
            if (!mergedProperties.find(f => f.path === property.path)) {
                mergedProperties = [...mergedProperties, property];
            }
        });

        taskProperties.forEach(property => {
            if (!mergedProperties.find(f => f.path === property.path)) {
                mergedProperties = [...mergedProperties, property];
            }
        });

        filterProperties.forEach(property => {
            if (!mergedProperties.find(f => f.path === property.path)) {
                mergedProperties = [...mergedProperties, property];
            }
        });

        return mergedProperties;
    }

    const getUsedUuids = () => {
        return [];
    }

    const setActions = (actions) => {
        setState({...state, actions: actions});
    }

    const setProperties = (properties) => {
        const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums, theme);
        let tree = QbUtils.checkTree(getInitialTree(props.activeLogic, null, config), config);
        setState({...state, properties: properties, logic: props.activeLogic, tree: tree, config: config});
    }

    const onRulesChange = (tree, config) => {
        const logic = QbUtils.jsonLogicFormat(tree, config).logic;
        setState({...state, logic: logic, tree: tree, config: config});
    }

    const onMakeDialogSmaller = () => {
        if (dialogSize === 'lg') {
            setDialogSize('md');
            setTaskExecutionDialogSize('md');
        }
    }

    const onMakeDialogLarger = () => {
        if (dialogSize === 'md') {
            setDialogSize('lg');
            setTaskExecutionDialogSize('lg');
        }
    }

    return (
        <StyledDialog
            onClose={handleClose} // Enable this to close editor by clicking outside the dialog
            aria-labelledby="task-execution-dialog-title"
            open={props.open}
            fullWidth={true}
            maxWidth={dialogSize}  //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
        >
            <DialogTitle
                id="customized-dialog-title"
                onClose={handleClose}
                onMakeDialogSmaller={dialogSize === 'lg' ? onMakeDialogSmaller : null}
                onMakeDialogLarger={dialogSize === 'md' ? onMakeDialogLarger : null}
            >
                <div className={classes.oneLine}>
                    <Typography variant="h6">{tasksEnabled ? 'Task execution' : 'Reservation'}</Typography>
                    {tasksEnabled &&
                        <TaskMenuSelector
                            tasks={getBucketTasks(props.bucket, accessContext.tasks)}
                            onTaskSelected={onTaskSelected}
                        />}
                    <Tabs
                        className={classes.tabs}
                        textColor="secondary"
                        indicatorColor="secondary"
                        value={activeTab}
                        onChange={handleChangedTab}
                        centered
                    >
                        <StyledTab label="Action"/>
                        <StyledTab label="Rules"/>
                        {tasksEnabled && <StyledTab label="Properties"/>}
                    </Tabs>
                    <div className={classes.devGrabSpace}/>
                </div>
            </DialogTitle>
            <EnumsProvider>
                <DialogContent
                    dividers
                    style={{height: '62vh'}}
                    ref={dialogContentRef}
                    classes={{
                        root: classes.root
                    }}
                >
                    {props.open && state.processing &&
                        <Grid
                            container
                            spacing={0}
                            direction="column"
                            alignItems="center"
                            justifyContent="center"
                            style={{minHeight: '50vh'}}
                        >
                            <Grid item xs={3}>
                                <CircularProgress disableShrink/>
                            </Grid>
                        </Grid>
                    }
                    {props.open && !state.processing && activeTab === 0 &&
                        <TaskActions
                            actions={state.actions}
                            properties={state.properties}
                            tags={getBucketTags(props.bucket, accessContext.tags)}
                            onChange={setActions}
                            pageSize={null}
                            taskTypes={tasksEnabled}
                            reserveType={reservationEnabled}
                            historyType={props.bucket.history === true}
                            owners={hasAdminRole() ? (accessContext.users ?? []) : null}
                        />}

                    {props.open && !state.processing && activeTab === 1 &&
                        <Box sx={{margin: 0}}>
                            <Query
                                {...state.config}
                                value={state.tree}
                                onChange={onRulesChange}
                                renderBuilder={renderBuilder}
                            />
                            <ApiRequestPanel bucket={props.bucket} logic={state.logic}
                                             sql={renderResult({tree: state.tree, config: state.config})}
                                             {...getActionRequest()}/>
                        </Box>
                    }

                    {props.open && !state.processing && activeTab === 2 &&
                        <PropertiesTable
                            used={getUsedUuids()}
                            data={state.properties}
                            enums={accessContext.enums}
                            onChange={setProperties}
                            title={'Class origin and defined properties:'}
                            pageSize={null}
                            parentContentRef={dialogContentRef}
                        />}
                </DialogContent>
            </EnumsProvider>
            <DialogActions
                classes={{
                    root: classes.root2
                }}>
                <Typography
                    color={'primary'}>{`${appliesCount} ${freeOnly ? 'free ' : ''}data ${appliesCount === 1 ? 'row matches' : 'rows match'} the rules`}</Typography>
                <div className={classes.divActionGrabSpace}/>
                <Button
                    variant="contained"
                    color="primary"
                    onClick={onTaskExecute}
                    disabled={state.processing || state.actions.type == null
                        || (state.actions.type === 'reserve' && !isReserveValid(state.actions))}
                >
                    Execute
                </Button>
            </DialogActions>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </StyledDialog>
    );
}


const StyledTab = Tab
