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
import {
    getSettingsTabHooverBackgroundColor, getSettingsTabSelectedColor,
} from "../../utils/MaterialTableHelper";
import EnumsContext from "../../context/enums/EnumsContext";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getColumnMapper} from "../../utils/NullValueMappers";
import {Tabs} from "@mui/material";
import Tab from "@mui/material/Tab";
import PropertiesTable, {mergeProperties} from "../utils/PropertiesTable";
import ColumnsTable from "../utils/ColumnsTable";
import MuiDialogActions from "@mui/material/DialogActions";

const styles = (theme) => ({
    root: {
        margin: 0,
        marginLeft: 15,
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

const useStyles = makeStyles(() => ({
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


EditColumnsDialog.propTypes = {
    configuration: PropTypes.object.isRequired,
    dataClass: PropTypes.object,
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function EditColumnsDialog(props) {

    const classes = useStyles();
    const [properties, setProperties] = useState(null);
    const [columns, setColumns] = useState(convertNullValuesInCollection(props.configuration.columns, getColumnMapper()));
    const [open, setOpen] = useState(false);
    const [activeTab, setActiveTab] = useState(0);
    const enumsContext = useContext(EnumsContext);
    const {enums, fetchEnums} = enumsContext;
    const dialogContentRef = React.useRef(null);

    useEffect(() => {
        setProperties(mergeProperties(props.configuration.properties, props.dataClass));
    }, [props.configuration.properties, props.dataClass]);

    useEffect(() => {
        if (enums == null)
            fetchEnums();
    }, [enums, fetchEnums]);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange({properties, columns});
        setOpen(false);
    }

    const handleChangedTab = (event, newActiveTab) => {
        setActiveTab(newActiveTab);
    }

    const getUsedUuids = () => {
        return columns.map(column => column.uuid);
    }

    return (
        <div>
            <Tooltip title={'Define columns'}>
                <Button
                    endIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                >
                    {`${props.configuration.columns.length}`}
                </Button>
            </Tooltip>
            <Dialog
                onClose={handleSave}
                aria-labelledby="customized-dialog-title"
                open={open}
                fullWidth={true}
                maxWidth='lg' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            >
                <DialogTitle id="customized-dialog-title" onClose={handleSave}>
                    <div className={classes.oneLine}>
                        <Typography variant="h6">{'Define the list of columns'}</Typography>
                        <Tabs
                            className={classes.tabs}
                            value={activeTab}
                            onChange={handleChangedTab}
                            centered
                        >

                            <StyledTab label="Columns"/>
                            <StyledTab label="Properties"/>
                        </Tabs>
                        <div className={classes.devGrabSpace}/>
                    </div>
                </DialogTitle>
                <DialogContent dividers style={{height: '75vh'}} ref = {dialogContentRef}>
                    {activeTab === 0 &&
                    <ColumnsTable
                        columns={columns}
                        properties={properties}
                        onChange={setColumns}
                        parentContentRef={dialogContentRef}
                    />}
                    {activeTab === 1 &&
                    <PropertiesTable
                        used={getUsedUuids()}
                        data={properties}
                        enums={enums}
                        onChange={setProperties}
                        title={'Class origin and defined properties:'}
                        parentContentRef={dialogContentRef}
                    />}
                </DialogContent>
                <DialogActions/>
            </Dialog>
        </div>
    );
}

const tabsStyles = theme => ({
    root: {
        "&:hover": {
            backgroundColor: getSettingsTabHooverBackgroundColor(theme),
            opacity: 1
        },
        "&$selected": {
            color: getSettingsTabSelectedColor(theme),
        },
        textTransform: "initial"
    },
    selected: {}
});

const StyledTab = withStyles(tabsStyles)(Tab)