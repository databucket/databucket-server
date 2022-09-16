import React, {useContext, useEffect, useRef, useState} from 'react';
import PropTypes from 'prop-types';
import Dialog from '@material-ui/core/Dialog';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import {iconsNames} from "../utils/AvailableIcons";
import {TextField} from "@material-ui/core";
import Grid from "@material-ui/core/Grid";
import Button from "@material-ui/core/Button";
import Divider from "@material-ui/core/Divider";
import Link from "@material-ui/core/Link";
import StyledIconButton from "../utils/StyledIconButton";
import {getButtonColor} from "../../utils/MaterialTableHelper";
import {useTheme} from "@material-ui/core/styles";
import SvgContext from "../../context/svgs/SvgContext";
import {MessageBox} from "../utils/MessageBox";
import {parseCustomSvg} from "../utils/SvgHelper";
import ColorPicker from "material-ui-color-picker";
import Typography from "@material-ui/core/Typography";

SimpleDialog.propTypes = {
    onClose: PropTypes.func.isRequired,
    open: PropTypes.bool.isRequired,
    initIcon: PropTypes.object.isRequired,
};

function SimpleDialog(props) {

    const theme = useTheme();
    const {onClose, initIcon, ...other} = props;
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [currentIcon, setCurrentIcon] = useState({name: initIcon.name, color: initIcon.color, svg: initIcon.svg});
    const svgContext = useContext(SvgContext);
    const {svgs, fetchSvgs} = svgContext;
    const colorPickerRef = useRef();

    useEffect(() => {
        if (svgs == null)
            fetchSvgs();
    }, [svgs, fetchSvgs]);

    function handleClose() {
        onClose(currentIcon);
    }

    function handleItemClick(iconName) {
        document.getElementById('iconNameTextBox').value = iconName;
        setCurrentIcon({...currentIcon, name: iconName, svg: null});
    }

    function handleSvgItemClick(svgIconItem) {
        document.getElementById('iconNameTextBox').value = svgIconItem.name;
        setCurrentIcon({...currentIcon, name: svgIconItem.name, svg: svgIconItem.structure});
    }

    const handleSave = () => {
        onClose(currentIcon);
    };

    const handleChangedIconName = (event) => {
        setCurrentIcon({...currentIcon, name: event.target.value, svg: null});
    };

    const handleRemoveColor = () => {
        setCurrentIcon({...currentIcon, color: null});
    }

    const handleChangedColor = (newColor) => {
        if (newColor != null)
            setCurrentIcon({...currentIcon, color: newColor});
    }

    return (
        <Dialog
            maxWidth='md' //'xs' | 'sm' | 'md' | 'lg' | 'xl' | false
            onClose={handleClose}
            aria-labelledby="simple-dialog-title"
            {...other}
        >
            <div style={{height: "10px"}}/>
            <Grid container spacing={0} alignItems="center">
                <Grid item xs>
                    <Typography style={{marginLeft: "20px"}}>Color:</Typography>
                </Grid>
                <Grid item xs={1}>
                    {currentIcon.color === null && (
                        <ColorPicker
                            ref={colorPickerRef}
                            onChange={handleChangedColor}
                            defaultValue={getButtonColor(theme)}
                        />)}
                    {currentIcon.color !== null && (
                        <ColorPicker
                            ref={colorPickerRef}
                            onChange={handleChangedColor}
                            defaultValue={currentIcon.color}
                        />)}
                </Grid>
                <Grid item xs>
                    <Tooltip id="reset-color" title="Reset color">
                        <IconButton color={"inherit"} onClick={handleRemoveColor}>
                            <span className="material-icons">format_color_reset</span>
                        </IconButton>
                    </Tooltip>
                </Grid>
                <Grid item xs={9}/>
            </Grid>
            <div style={{height: "10px"}}/>
            <Divider/>
            <div>
                {iconsNames.map((iName, key) => (
                    <Tooltip title={iName} key={key}>
                        <IconButton onClick={() => handleItemClick(iName)} color={"inherit"}>
                            <span className="material-icons">{iName}</span>
                        </IconButton>
                    </Tooltip>
                ))}
            </div>
            <div style={{height: "1px"}}/>
            <Divider/>
            {svgs != null && (
                <div>
                    {
                        svgs.map((svg) => (
                            <Tooltip title={svg.name} key={svg.id}>
                                <IconButton onClick={() => handleSvgItemClick(svg)}>
                                    {parseCustomSvg(svg.structure, getButtonColor(theme))}
                                </IconButton>
                            </Tooltip>
                        ))
                    }
                </div>
            )}
            <div style={{height: "1px"}}/>
            {svgs != null && <Divider/>}
            <div style={{height: "20px"}}/>
            <div>
                <Grid container spacing={0} alignItems="center">
                    <Grid item xs={2}/>
                    <Grid item xs>
                        <div style={{marginLeft: "20px", maxWidth: "50px"}}>
                            <Tooltip id="select icon" title="Selected icon">
                                <StyledIconButton
                                    iconName={currentIcon.name}
                                    iconColor={currentIcon.color != null ? currentIcon.color : getButtonColor(theme)}
                                    iconSvg={currentIcon.svg}
                                />
                            </Tooltip>
                        </div>
                    </Grid>
                    <Grid item xs={4}>
                        <TextField
                            hiddenLabel
                            id="iconNameTextBox"
                            size="small"
                            fullWidth
                            placeholder={currentIcon.name}
                            onChange={handleChangedIconName}
                        />
                    </Grid>
                    <Grid item xs={3}>
                        <Link style={{marginLeft: "10px"}} target='_blank' href='https://fonts.google.com/icons?selected=Material+Icons' color="primary">More icons...</Link><br/>
                    </Grid>
                    <Grid item xs={1}/>
                    <Grid item xs>
                        <Button id="saveButton" onClick={handleSave} color="primary">
                            Save
                        </Button>
                    </Grid>
                </Grid>
            </div>
            <div style={{height: "5px"}}/>
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </Dialog>
    );
}

SelectIconDialog.propTypes = {
    icon: PropTypes.object.isRequired,
    onChange: PropTypes.func
};

export default function SelectIconDialog(props) {
    const theme = useTheme();
    const {icon, onChange} = props;
    const [open, setOpen] = React.useState(false);
    const [selectedIcon, setSelectedIcon] = React.useState({name: icon.name, color: icon.color, svg: icon.svg});

    function handleClickOpen() {
        setOpen(true);
    }

    const handleClose = newIcon => {
        setOpen(false);
        setSelectedIcon(newIcon);
        onChange(newIcon);
    };

    return (
        <div>
            <Tooltip title={'Change icon'}>
                <StyledIconButton
                    onClick={handleClickOpen}
                    iconName={selectedIcon.name}
                    iconColor={selectedIcon.color != null ? selectedIcon.color : getButtonColor(theme)}
                    iconSvg={selectedIcon.svg}
                />
            </Tooltip>
            <SimpleDialog initIcon={selectedIcon} open={open} onClose={handleClose}/>
        </div>
    );
}
