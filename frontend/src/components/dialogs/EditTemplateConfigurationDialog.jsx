import React, {useEffect, useState} from 'react';
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
import TemplateTabs from "../management/templatesConfig/_TemplConfigTabs";

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


EditTemplateConfigurationDialog.propTypes = {
    name: PropTypes.string.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
}

export default function EditTemplateConfigurationDialog(props) {

    const [open, setOpen] = useState(false);
    const [template, setTemplate] = useState(null);

    // must be initiated by hook
    useEffect(() => {
        setTemplate(props.rowData);
    }, [props.rowData]);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange(template['configuration']);
        setOpen(false);
    }

    return (
        <div>
            <Tooltip title='Configuration'>
                <Button
                    endIcon={<MoreHoriz/>}
                    onClick={handleClickOpen}
                >
                    {``}
                </Button>
            </Tooltip>
            <Dialog
                onClose={handleSave}
                aria-labelledby="customized-dialog-title"
                open={open}
                fullWidth={true}
                maxWidth='xl' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            >
                <DialogTitle id="customized-dialog-title" onClose={handleSave}>
                    {`Template: ${props.name}`}
                </DialogTitle>
                <DialogContent dividers>
                    <TemplateTabs
                        template={template}
                        setTemplate={setTemplate}
                    />
                </DialogContent>
            </Dialog>
        </div>
    );
}