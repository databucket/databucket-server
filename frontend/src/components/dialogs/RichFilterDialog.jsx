import React, {useCallback, useContext, useEffect, useState} from 'react';
import {styled, useTheme} from '@mui/material/styles';
import Dialog from '@mui/material/Dialog';
import IconButton from '@mui/material/IconButton';
import MuiDialogTitle from '@mui/material/DialogTitle';
import MuiDialogContent from '@mui/material/DialogContent';
import MuiDialogActions from '@mui/material/DialogActions';
import CloseIcon from '@mui/icons-material/Close';
import Typography from '@mui/material/Typography';
import PropTypes from "prop-types";
import {Box, Grid, Tabs} from "@mui/material";
import {getPostOptions} from "../../utils/MaterialTableHelper";
import Tab from "@mui/material/Tab";
import {MessageBox} from "../utils/MessageBox";
import PropertiesTable from "../utils/PropertiesTable";
import {getBucketFilters, getBucketTags} from "../data/BucketDataTableHelper";
import EnumsProvider from "../../context/enums/EnumsProvider";
import Button from "@mui/material/Button";
import {handleErrors} from "../../utils/FetchHelper";
import {getDataUrl} from "../../utils/UrlBuilder";
import {getClassById} from "../../utils/JsonHelper";
import {Query, Utils as QbUtils} from "@react-awesome-query-builder/mui";
import {createConfig, getInitialTree, renderBuilder, renderResult} from "../utils/QueryBuilderHelper";
import AccessContext from "../../context/access/AccessContext";
import FilterMenuSelector from "../data/FilterMenuSelector";
import {getDataFilterDialogSize, setDataFilterDialogSize} from "../../utils/ConfigurationStorage";
import {debounce} from "../utils/Debouncer";

const PREFIX = 'RichFilterDialog';

const classes = {
    root: `${PREFIX}-root`,
    root2: `${PREFIX}-root2`,
    root3: `${PREFIX}-root3`,
    selected: `${PREFIX}-selected`,
    dialogPaper: `${PREFIX}-dialogPaper`,
    tabs: `${PREFIX}-tabs`,
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

    [`& .${classes.tabs}`]: {
        flexGrow: 1
    },

    [`& .${classes.divActionGrabSpace}`]: {
        width: '30px'
    },
    [`& .${classes.root}`]: {
        margin: 0,
        padding: theme.spacing(2),
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

const StyledDialogTitle = styled(MuiDialogTitle)(({theme}) => ({
    margin: 0,
    padding: theme.spacing(2),
}));

const DialogTitleGrid = (props => {
    const {children, onClose, onMakeDialogSmaller, onMakeDialogLarger} = props;
    return (
        <StyledDialogTitle>
            <Grid container direction="row" sx={{alignItems: 'center'}}>
                {children}
                <Grid item xs>
                    <Box sx={{display: 'flex', justifyContent: "flex-end"}}>
                        <IconButton
                            aria-label="Smaller"
                            onClick={onMakeDialogSmaller}
                            color={"inherit"}
                            disabled={onMakeDialogSmaller == null}
                            size="large">
                            <span className="material-icons">fullscreen_exit</span>
                        </IconButton>
                        <IconButton
                            aria-label="Larger"
                            onClick={onMakeDialogLarger}
                            color={"inherit"}
                            disabled={onMakeDialogLarger == null}
                            size="large">
                            <span className="material-icons">fullscreen</span>
                        </IconButton>
                        {onClose ? (
                            <IconButton
                                aria-label="Close"
                                onClick={onClose}
                                size="large">
                                <CloseIcon/>
                            </IconButton>
                        ) : null}
                    </Box>
                </Grid>
            </Grid>
        </StyledDialogTitle>
    );
});

const TopPaddedGridItem = styled(Grid)(({theme}) => ({
    padding: theme.spacing(1)
}));

const DialogContent = MuiDialogContent;

const DialogActions = MuiDialogActions;

RichFilterDialog.propTypes = {
    open: PropTypes.bool.isRequired,
    bucket: PropTypes.object.isRequired,
    onClose: PropTypes.func.isRequired,
    activeLogic: PropTypes.object,
    setActiveLogic: PropTypes.func.isRequired
};

export default function RichFilterDialog(props) {

    const theme = useTheme();
    const accessContext = useContext(AccessContext);
    const bucketTags = getBucketTags(props.bucket, accessContext.tags);
    const [activeTab, setActiveTab] = useState(0);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [appliesCount, setAppliesCount] = useState(0);
    const [state, setState] = useState({properties: [], logic: {}, tree: {}, config: {}});
    const [dialogSize, setDialogSize] = useState('md');
    const dialogContentRef = React.useRef(null);

    useEffect(() => {
        setDialogSize(getDataFilterDialogSize());
    }, []);

    useEffect(() => {
        if (props.open) {
            setActiveTab(0);
            const properties = getClassProperties();
            const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums, theme);
            const disabledRulesLogic = makeRulesDisabled(props.activeLogic);
            if (Object.keys(state.tree).length === 0) {
                let tree = QbUtils.checkTree(getInitialTree(disabledRulesLogic, null, config), config);
                setState({
                    ...state,
                    properties: getClassProperties(),
                    logic: disabledRulesLogic,
                    tree: tree,
                    config: config
                });
            } else {
                setState({
                    ...state,
                    properties: getClassProperties(),
                    logic: disabledRulesLogic,
                    tree: state.tree,
                    config: config
                });
            }
        }
    }, [props.open]);

    const makeRulesDisabled = (inputLogic) => {
        // TODO _meta << not implemented by component founder yet!!! (https://github.com/ukrbublik/react-awesome-query-builder/issues/377)
        // const inputLogicStr = JSON.stringify(inputLogic);
        // const disabledInputLogicStr = inputLogicStr.replace("\"var\":", "\"_meta\": {\"readonly\": true}, \"var\":");
        // console.log(JSON.stringify(inputLogic));
        // console.log(JSON.parse(disabledInputLogicStr));
        // return JSON.parse(disabledInputLogicStr);
        return inputLogic;
    }

    const filterApply = () => {
        props.setActiveLogic(state.logic);
    }

    const getMergedProperties = (classProperties, filterProperties) => {
        let mergedProperties = [];

        classProperties.forEach(property => {
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

    const onFilterSelected = (filter) => {
        const properties = getMergedProperties(getClassProperties(), filter.configuration.properties);
        const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums);
        const tree = QbUtils.checkTree(getInitialTree(filter.configuration.logic, filter.configuration.tree, config), config);
        setState({
            ...state,
            logic: filter.configuration.logic,
            tree: tree,
            properties: properties,
            config: config
        });
    }

    useEffect(() => {
        refreshAppliesCount({open: props.open, bucket: props.bucket, logic: state.logic});
    }, [props.open, state.logic]);

    const refreshAppliesCount = useCallback(
        debounce(({open, bucket, logic}) => {
            if (open) {
                let resultOk = true;
                fetch(getDataUrl(bucket) + '/get?limit=0', getPostOptions({logic}))
                    .then(handleErrors)
                    .catch(error => {
                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                        resultOk = false;
                    })
                    .then(result => {
                        if (resultOk)
                            setAppliesCount(result.total);
                    });
            }
        }, 1000),
        []
    );

    const handleChangedTab = (event, newActiveTab) => {
        setActiveTab(newActiveTab);
    }

    const handleClose = () => {
        props.onClose();
    };

    const getClassProperties = () => {
        if (props.bucket.classId != null) {
            const dataClass = getClassById(accessContext.classes, props.bucket.classId);
            return dataClass.configuration;
        } else
            return [];
    }

    const setProperties = (properties) => {
        const config = createConfig(properties, bucketTags, accessContext.users, accessContext.enums);
        const disabledRulesLogic = makeRulesDisabled(props.activeLogic);
        let tree = QbUtils.checkTree(getInitialTree(disabledRulesLogic, null, config), config);
        setState({...state, properties: properties, logic: disabledRulesLogic, tree: tree, config: config});
    }

    const onRulesChange = (tree, config) => {
        const logic = QbUtils.jsonLogicFormat(tree, config).logic;
        setState({...state, logic: logic, tree: tree, config: config});
    }

    const onMakeDialogSmaller = () => {
        if (dialogSize === 'lg') {
            setDialogSize('md');
            setDataFilterDialogSize('md');
        }
    }

    const onMakeDialogLarger = () => {
        if (dialogSize === 'md') {
            setDialogSize('lg');
            setDataFilterDialogSize('lg');
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
            <DialogTitleGrid
                id="customized-dialog-title"
                onClose={handleClose}
                onMakeDialogSmaller={dialogSize === 'lg' ? onMakeDialogSmaller : null}
                onMakeDialogLarger={dialogSize === 'md' ? onMakeDialogLarger : null}
            >
                <TopPaddedGridItem item>
                    {'Data filter'}
                </TopPaddedGridItem>
                <TopPaddedGridItem item xs>
                    <FilterMenuSelector filters={getBucketFilters(props.bucket, accessContext.filters)}
                                        onFilterSelected={onFilterSelected}/>
                </TopPaddedGridItem>
                <Grid item xs={6}>
                    <Tabs
                        className={classes.tabs}
                        value={activeTab}
                        onChange={handleChangedTab}
                    >
                        <StyledTab label="Rules"/>
                        <StyledTab label="Properties"/>
                    </Tabs>
                </Grid>
            </DialogTitleGrid>
            <EnumsProvider>
                <DialogContent
                    dividers
                    style={{height: '62vh'}}
                    ref={dialogContentRef}
                    classes={{
                        root: classes.root
                    }}>
                    {props.open && activeTab === 0 && Object.keys(state.tree).length > 0 &&
                        <div>
                            <Query
                                {...state.config}
                                value={state.tree}
                                onChange={onRulesChange}
                                renderBuilder={renderBuilder}
                            />
                            {renderResult({tree: state.tree, config: state.config})}
                        </div>
                    }

                    {props.open && activeTab === 1 &&
                        <PropertiesTable
                            used={[]}
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
                    color={'primary'}>{`${appliesCount} data ${appliesCount > 1 ? 'rows' : 'row'} ${appliesCount > 1 ? 'match' : 'matches'} the rules`}</Typography>
                <div className={classes.divActionGrabSpace}/>
                <Button
                    variant="contained"
                    color="primary"
                    onClick={filterApply}
                >
                    Apply
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
