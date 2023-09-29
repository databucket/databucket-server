import React, {useEffect, useState} from 'react';
import {
    Button,
    Dialog,
    DialogActions as MuiDialogActions,
    DialogContent as MuiDialogContent,
    DialogTitle as MuiDialogTitle,
    IconButton,
    styled,
    Tab,
    Tabs,
    Tooltip,
    Typography
} from '@mui/material';
import {Close as CloseIcon, MoreHoriz} from '@mui/icons-material';
import PropTypes from 'prop-types';
import {MessageBox} from "../utils/MessageBox";
import PropertiesTable, {mergeProperties} from "../utils/PropertiesTable";
import TaskActionsTemplate from "../utils/TaskActionsTemplate";

const PREFIX = 'TaskEditConfigDialogTemplate';

const classes = {
    root: `${PREFIX}-root`,
    root2: `${PREFIX}-root2`,
    root3: `${PREFIX}-root3`,
    selected: `${PREFIX}-selected`,
    dialogPaper: `${PREFIX}-dialogPaper`,
    oneLine: `${PREFIX}-oneLine`,
    tabs: `${PREFIX}-tabs`,
    devGrabSpace: `${PREFIX}-devGrabSpace`,
    closeButton: `${PREFIX}-closeButton`
};

const Root = styled('div')(({theme}) => ({
    [`& .${classes.dialogPaper}`]: {
        minHeight: '80vh',
    },

    [`& .${classes.oneLine}`]: {
        display: 'flex',
        alignItems: 'center',
        flexWrap: 'wrap'
    },

    [`& .${classes.tabs}`]: {
        flexGrow: 1
    },

    [`& .${classes.devGrabSpace}`]: {
        width: '200px'
    },
    [`& .${classes.root}`]: {
        margin: 0,
        padding: theme.spacing(1),
    },
    [`& .${classes.closeButton}`]: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1),
        color: theme.palette.grey[500],
    }
}));

const DialogTitle = ((props) => {
    const {children, onClose, ...other} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root} {...other}>
            {children}
            {onClose ? (
                <IconButton
                    aria-label="close"
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

TaskEditConfigDialogTemplate.propTypes = {
    rowData: PropTypes.object.isRequired,
    configuration: PropTypes.object.isRequired, // actions, properties
    name: PropTypes.string.isRequired,
    dataClass: PropTypes.object,
    onChange: PropTypes.func.isRequired,
    enums: PropTypes.array.isRequired,
    tags: PropTypes.array.isRequired
}

export default function TaskEditConfigDialogTemplate(props) {

    const [activeTab, setActiveTab] = useState(0);
    const [messageBox, setMessageBox] = useState(
        {open: false, severity: 'error', title: '', message: ''})
    const [open, setOpen] = useState(false);
    const [actions, setActions] = useState(props.configuration.actions);
    const [properties, setProperties] = useState(null);
    const [filteredTags, setFilteredTags] = useState(null);
    const dialogContentRef = React.useRef(null);

    useEffect(() => {
        const fTags = props.tags.filter(tag =>
            (Array.isArray(tag.bucketsIds) && Array.isArray(
                props.rowData.bucketsIds) && tag.bucketsIds.some(
                item => props.rowData.bucketsIds.includes(item))) ||
            (tag.classesIds != null && tag.classesIds.includes(
                parseInt(props.rowData.classId, 10)))
        );
        setFilteredTags(fTags);
    }, [props.tags, props.rowData]);

    useEffect(() => {
        setProperties(
            mergeProperties(props.configuration.properties, props.dataClass));
    }, [props.configuration.properties, props.dataClass]);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange({actions, properties});
        setOpen(false);
    }

    const handleChangedTab = (event, newActiveTab) => {
        setActiveTab(newActiveTab);
    }

    const getUsedUuids = () => {
        return actions.properties.map(property => property.uuid);
    }

    return (
        <Root>
            <Tooltip title={'Define action'}>
                <Button
                    endIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                    style={{textTransform: 'none'}}
                >
                    {getActionsType(actions)}
                </Button>
            </Tooltip>
            <Dialog
                onClose={handleSave}
                aria-labelledby="customized-dialog-title"
                classes={{paper: classes.dialogPaper}}
                open={open}
                fullWidth={true}
                maxWidth='lg' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            >
                <DialogTitle id="customized-dialog-title" onClose={handleSave}>
                    <div className={classes.oneLine}>
                        <Typography
                            variant="h6">{'Task configuration'}</Typography>
                        <Tabs
                            className={classes.tabs}
                            value={activeTab}
                            onChange={handleChangedTab}
                            centered
                        >
                            <StyledTab label="Action"/>
                            <StyledTab label="Properties"/>
                        </Tabs>
                        <div className={classes.devGrabSpace}/>
                    </div>
                </DialogTitle>
                <DialogContent
                    dividers
                    style={{height: '75vh'}}
                    ref={dialogContentRef}
                    classes={{
                        root: classes.root
                    }}>
                    {open && activeTab === 0 &&
                        <TaskActionsTemplate
                            actions={actions}
                            properties={properties}
                            tags={filteredTags}
                            onChange={setActions}
                            pageSize={null}
                            customHeight={20}
                            enums={props.enums}
                        />}
                    {open && activeTab === 1 &&
                        <PropertiesTable
                            used={getUsedUuids()}
                            data={properties}
                            enums={props.enums}
                            onChange={setProperties}
                            title={'Class origin and defined properties:'}
                            pageSize={null}
                            parentContentRef={dialogContentRef}
                        />}
                </DialogContent>
                <DialogActions
                    classes={{
                        root: classes.root2
                    }}/>
            </Dialog>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </Root>
    );
}

const StyledTab = Tab

export const getActionsType = (actions) => {
    if (actions.type != null) {
        return actions.type;
    } else {
        return '- none -';
    }
}
