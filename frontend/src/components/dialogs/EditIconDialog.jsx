import React from 'react';
import PropTypes from 'prop-types';
import Dialog from '@material-ui/core/Dialog';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import DynamicIcon from '../utils/DynamicIcon';
import {iconsNames} from "../utils/AvailableIcons";

SimpleDialog.propTypes = {
    onClose: PropTypes.func,
    open: PropTypes.bool,
    selectedValue: PropTypes.string,
};

function SimpleDialog(props) {
    const {onClose, selectedValue, ...other} = props;

    function handleClose() {
        onClose(selectedValue);
    }

    function handleItemClick(value) {
        onClose(value);
    }

    return (
        <Dialog
            maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onClose={handleClose}
            aria-labelledby="simple-dialog-title"
            {...other}
        >
            <div>
                {iconsNames.map((iName, key) => (
                    <Tooltip title={iName} key={key}>
                        <IconButton onClick={() => handleItemClick(iName)}>
                            <span className="material-icons" style={{fontSize: 29}}>{iName}</span>
                        </IconButton>
                    </Tooltip>
                ))}
            </div>
        </Dialog>
    );
}

SimpleDialog.propTypes = {
    onChange: PropTypes.func
};

export default function SelectIconDialog(props) {
    const {value, onChange} = props;
    const [open, setOpen] = React.useState(false);
    const [selectedValue, setSelectedValue] = React.useState(value);

    function handleClickOpen() {
        setOpen(true);
    }

    const handleClose = value => {
        setOpen(false);
        setSelectedValue(value);
        onChange(value);
    };

    return (
        <div>
            <Tooltip title={'Change icon'}>
                <IconButton onClick={handleClickOpen}>
                    <DynamicIcon iconName={selectedValue}/>
                </IconButton>
            </Tooltip>
            <SimpleDialog selectedValue={selectedValue} open={open} onClose={handleClose}/>
        </div>
    );
}
