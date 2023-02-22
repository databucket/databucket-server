import React, {useContext, useEffect, useState} from 'react';
import makeStyles from '@mui/styles/makeStyles';
import withStyles from '@mui/styles/withStyles';
import Dialog from '@mui/material/Dialog';
import MuiDialogTitle from '@mui/material/DialogTitle';
import MuiDialogContent from '@mui/material/DialogContent';
import IconButton from '@mui/material/IconButton';
import CloseIcon from '@mui/icons-material/Done';
import Typography from '@mui/material/Typography';
import MoreHoriz from "@mui/icons-material/MoreHoriz";
import Tooltip from "@mui/material/Tooltip";
import PropTypes from 'prop-types';
import Button from "@mui/material/Button";
import {MessageBox} from "../utils/MessageBox";
import {Tabs} from "@mui/material";
import {getSettingsTabHooverBackgroundColor, getSettingsTabSelectedColor} from "../../utils/MaterialTableHelper";
import Tab from "@mui/material/Tab";
import TaskActions from "../utils/TaskActions";
import PropertiesTable, {mergeProperties} from "../utils/PropertiesTable";
import TagsContext from "../../context/tags/TagsContext";
import EnumsContext from "../../context/enums/EnumsContext";
import MuiDialogActions from "@mui/material/DialogActions";


const styles = (theme) => ({
    root: {
        margin: 0,
        padding: theme.spacing(1),
    },
    closeButton: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1),
        color: theme.palette.grey[500],
    }
});

const DialogTitle = withStyles(styles)((props) => {
    const {children, classes, onClose, ...other} = props;
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

const DialogContent = withStyles((theme) => ({
    root: {
        padding: theme.spacing(0),
    },
}))(MuiDialogContent);

const DialogActions = withStyles(theme => ({
    root: {
        margin: 0,
        padding: theme.spacing(1),
    },
}))(MuiDialogActions);

TaskEditConfigDialog.propTypes = {
    rowData: PropTypes.object.isRequired,
    configuration: PropTypes.object.isRequired, // actions, properties
    name: PropTypes.string.isRequired,
    dataClass: PropTypes.object,
    onChange: PropTypes.func.isRequired
}

export default function TaskEditConfigDialog(props) {

    const classes = useStyles();
    const [activeTab, setActiveTab] = useState(0);
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''})
    const [open, setOpen] = useState(false);
    const [actions, setActions] = useState(props.configuration.actions);
    const [properties, setProperties] = useState(null);
    const tagsContext = useContext(TagsContext);
    const {tags, fetchTags} = tagsContext;
    const [filteredTags, setFilteredTags] = useState(null);
    const enumsContext = useContext(EnumsContext);
    const {enums, fetchEnums} = enumsContext;
    const dialogContentRef = React.useRef(null);

    useEffect(() => {
        if (tags == null)
            fetchTags();
    }, [tags, fetchTags]);

    useEffect(() => {
        if (enums == null)
            fetchEnums();
    }, [enums, fetchEnums]);

    useEffect(() => {
        const fTags = tags.filter(tag =>
            (Array.isArray(tag.bucketsIds) && Array.isArray(props.rowData.bucketsIds) && tag.bucketsIds.some(item => props.rowData.bucketsIds.includes(item))) ||
            (tag.classesIds != null && tag.classesIds.includes(parseInt(props.rowData.classId, 10)))
        );
        setFilteredTags(fTags);
    }, [tags, props.rowData]);

    useEffect(() => {
        if (tags == null)
            fetchTags();
    }, [tags, fetchTags]);

    useEffect(() => {
        setProperties(mergeProperties(props.configuration.properties, props.dataClass));
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
        <div>
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
                        <Typography variant="h6">{'Task configuration'}</Typography>
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
                <DialogContent dividers style={{height:'75vh'}} ref = {dialogContentRef}>
                    {open && activeTab === 0 &&
                    <TaskActions
                        actions={actions}
                        properties={properties}
                        tags={filteredTags}
                        onChange={setActions}
                        pageSize={null}
                        customHeight={20}
                    />}
                    {open && activeTab === 1 &&
                    <PropertiesTable
                        used={getUsedUuids()}
                        data={properties}
                        enums={enums}
                        onChange={setProperties}
                        title={'Class origin and defined properties:'}
                        pageSize={null}
                        parentContentRef={dialogContentRef}
                    />}
                </DialogContent>
                <DialogActions />
            </Dialog>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}

const useStyles = makeStyles(() => ({
    dialogPaper: {
        minHeight: '80vh',
    },
    oneLine: {
        display: 'flex',
        alignItems: 'center',
        flexWrap: 'wrap'
    },
    tabs: {
        flexGrow: 1
    },
    devGrabSpace: {
        width: '200px'
    }
}));

const tabStyles = theme => ({
    root: {
        "&:hover": {
            backgroundColor: getSettingsTabHooverBackgroundColor(theme),
            opacity: 1
        },
        "&$selected": {
            // backgroundColor: getSettingsTabSelectedBackgroundColor(theme),
            color: getSettingsTabSelectedColor(theme),
        },
        textTransform: "initial"
    },
    selected: {}
});

const StyledTab = withStyles(tabStyles)(Tab)

export const getActionsType = (actions) => {
    if (actions.type != null)
        return actions.type;
    else
        return '- none -';
}