import React, { useEffect, useState } from 'react';
import {
    Button,
    Dialog,
    DialogContent as MuiDialogContent,
    DialogTitle as MuiDialogTitle,
    IconButton,
    styled,
    Tooltip,
    Typography
} from '@mui/material';
import { Close as CloseIcon, MoreHoriz } from '@mui/icons-material';
import PropTypes from 'prop-types';
import TemplateTabs from "../management/templatesConfig/_TemplConfigTabs";

const StyledDialogTitle = styled(MuiDialogTitle)(({ theme }) => ({
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    margin: 0,
    padding: theme.spacing(0),
    width: '100%',

    '& .titleText': {
        flexGrow: 1,
        marginLeft: theme.spacing(2),
        marginRight: theme.spacing(2),
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
    },

    '& .closeButton': {
        color: theme.palette.grey[500],
        padding: theme.spacing(2),
    },
}));

const DialogTitle = (props) => {
    const { children, onClose, ...other } = props;
    return (
        <StyledDialogTitle disableTypography {...other}>
            <Typography variant="h6" className="titleText">{children}</Typography>
            {onClose ? (
                <IconButton
                    aria-label="close"
                    className="closeButton"
                    onClick={onClose}
                    size="large"
                >
                    <CloseIcon />
                </IconButton>
            ) : null}
        </StyledDialogTitle>
    );
};

const StyledDialogContent = styled(MuiDialogContent)(({ theme }) => ({
    padding: 0,
}));

EditTemplateConfigurationDialog.propTypes = {
    name: PropTypes.string.isRequired,
    rowData: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
};

export default function EditTemplateConfigurationDialog(props) {
    const [open, setOpen] = useState(false);
    const [template, setTemplate] = useState(null);

    useEffect(() => {
        setTemplate(props.rowData);
    }, [props.rowData]);

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleSave = () => {
        props.onChange(template['configuration']);
        setOpen(false);
    };

    return (
        <div>
            <Tooltip title='Configuration'>
                <Button
                    endIcon={<MoreHoriz />}
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
                maxWidth='xl'
            >
                <DialogTitle id="customized-dialog-title" onClose={handleSave}>
                    {`Template: ${props.name}`}
                </DialogTitle>
                <StyledDialogContent dividers>
                    <TemplateTabs
                        template={template}
                        setTemplate={setTemplate}
                    />
                </StyledDialogContent>
            </Dialog>
        </div>
    );
}
