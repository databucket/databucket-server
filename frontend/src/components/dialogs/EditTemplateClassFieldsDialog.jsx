import React, {useContext, useEffect, useState} from 'react';
import {withStyles} from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Done';
import Typography from '@material-ui/core/Typography';
import MoreHoriz from "@material-ui/icons/MoreHoriz";
import Tooltip from "@material-ui/core/Tooltip";
import PropTypes from 'prop-types';
import Button from "@material-ui/core/Button";
import PropertiesTable from "../utils/PropertiesTable";
import MuiDialogActions from "@material-ui/core/DialogActions";
import TemplatesContext from "../../context/templates/TemplatesContext";
import {getTemplatesArtefacts} from "../management/templatesConfig/_TemplUtils";

const styles = (theme) => ({
    root: {
        margin: 0,
        padding: theme.spacing(2),
    },
    closeButton: {
        position: 'absolute',
        right: theme.spacing(1),
        top: theme.spacing(1),
        color: theme.palette.grey[500],
    },
});

const DialogTitle = withStyles(styles)((props) => {
    const {children, classes, onClose, ...other} = props;
    return (
        <MuiDialogTitle disableTypography className={classes.root} {...other}>
            <Typography variant="h6">{children}</Typography>
            {onClose ? (
                <IconButton aria-label="close" className={classes.closeButton} onClick={onClose}>
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

EditTemplateClassFieldsDialog.propTypes = {
    template: PropTypes.object.isRequired,
    configuration: PropTypes.array.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function EditTemplateClassFieldsDialog(props) {

    const [data, setData] = useState(props.configuration);
    const [open, setOpen] = useState(false);
    const dialogContentRef = React.useRef(null);
    const templatesContext = useContext(TemplatesContext);
    const {templates} = templatesContext;
    const [enums, setEnums] = useState([]);

    useEffect(() => {
        setEnums(getTemplatesArtefacts(props.template, templates, 'enums'));
    }, [templates]);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange(data.map(({title, path, type, enumId, uuid}) => ({title, path, type, enumId, uuid})));
        setOpen(false);
    }

    return (
        <div>
            <Tooltip title={'Configure properties'}>
                <Button
                    endIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                >
                    {`${props.configuration.length}`}
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
                    {'Define class properties'}
                </DialogTitle>
                <DialogContent dividers style={{height: '75vh'}} ref={dialogContentRef}>
                    {open &&
                    <PropertiesTable
                        data={data}
                        enums={enums}
                        onChange={setData}
                        title={''}
                        parentContentRef={dialogContentRef}
                    />
                    }
                </DialogContent>
                <DialogActions/>
            </Dialog>
        </div>
    );
}