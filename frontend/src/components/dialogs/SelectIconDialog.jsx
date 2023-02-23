import React, {createRef, forwardRef, useContext, useEffect, useRef, useState} from 'react';
import PropTypes from 'prop-types';
import Dialog from '@mui/material/Dialog';
import Tooltip from '@mui/material/Tooltip';
import IconButton from '@mui/material/IconButton';
import {iconsNames} from "../utils/AvailableIcons";
import {TextField} from "@mui/material";
import Grid from "@mui/material/Grid";
import Button from "@mui/material/Button";
import Divider from "@mui/material/Divider";
import Link from "@mui/material/Link";
import StyledIconButton from "../utils/StyledIconButton";
import {getButtonColor} from "../../utils/MaterialTableHelper";
import {useTheme} from "@mui/material/styles";
import SvgContext from "../../context/svgs/SvgContext";
import {MessageBox} from "../utils/MessageBox";
import {parseCustomSvg} from "../utils/SvgHelper";
import {MuiColorInput} from 'mui-color-input'
import Typography from "@mui/material/Typography";
import {DarkTheme, getAppBarBackgroundColor, LightTheme} from "../../utils/Themes";
import StyledIcon from "../utils/StyledIcon";

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
                        <MuiColorInput
                            ref={colorPickerRef}
                            onChange={handleChangedColor}
                            value={getButtonColor(theme)}
                        />)}
                    {currentIcon.color !== null && (
                        <MuiColorInput
                            ref={colorPickerRef}
                            onChange={handleChangedColor}
                            value={currentIcon.color}
                        />)}
                </Grid>
                <Grid item xs>
                    <Tooltip id="reset-color" title="Reset color">
                        <IconButton color={"inherit"} onClick={handleRemoveColor} size="large">
                            <span className="material-icons">format_color_reset</span>
                        </IconButton>
                    </Tooltip>
                </Grid>
                <Grid item xs={9}/>
            </Grid>
            <div style={{height: "10px"}}/>
            <Divider/>
            <div>
                {iconsNames.map((iName) => (
                    <Tooltip title={iName} key={iName}>
                        <IconButton onClick={() => handleItemClick(iName)} color={"inherit"} size="large">
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
                                <IconButton onClick={() => handleSvgItemClick(svg)} size="large">
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
                    <Grid item xs={1}/>
                    <Grid item xs>
                        <div style={
                            {
                                background: getAppBarBackgroundColor(),
                                height: "48px",
                                display: 'flex',
                                justifyContent: 'center',
                                alignItems: 'center'
                            }
                        }>
                            <StyledIcon
                                iconName={currentIcon.name}
                                iconColor={currentIcon.color}
                                iconSvg={currentIcon.svg}
                                themeType={'banner-display'}
                            />
                        </div>
                    </Grid>
                    <Grid item xs>
                        <div style={
                            {
                                background: LightTheme.palette.background.paper,
                                height: "48px",
                                display: 'flex',
                                justifyContent: 'center',
                                alignItems: 'center'
                            }
                        }>
                            <StyledIcon
                                iconName={currentIcon.name}
                                iconColor={currentIcon.color}
                                iconSvg={currentIcon.svg}
                                themeType={'light-display'}
                            />
                        </div>
                    </Grid>
                    <Grid item xs>
                        <div style={
                            {
                                background: DarkTheme.palette.background.paper,
                                height: "48px",
                                display: 'flex',
                                justifyContent: 'center',
                                alignItems: 'center'
                            }
                        }>
                            <StyledIcon
                                iconName={currentIcon.name}
                                iconColor={currentIcon.color}
                                iconSvg={currentIcon.svg}
                                themeType={'dark-display'}
                            />
                        </div>
                    </Grid>
                    <Grid item xs={1}/>
                    <Grid item xs={3}>
                        <TextField
                            hiddenLabel
                            id="iconNameTextBox"
                            size="small"
                            fullWidth
                            placeholder={currentIcon.name}
                            onChange={handleChangedIconName}
                        />
                    </Grid>
                    <Grid item xs={2}>
                        <Link style={{marginLeft: "10px"}} target='_blank'
                              href='https://fonts.google.com/icons?selected=Material+Icons' color="primary">More
                            icons...</Link><br/>
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

const TooltipIconButton = forwardRef((props, ref) =>
    <StyledIconButton{...props} ref={ref}/>);
const SomeContent = React.forwardRef((props, ref) =>
    <Button {...props} ref={ref}>Hello, World!</Button>);
export default function SelectIconDialog(props) {
    const theme = useTheme();
    const dialogRef = createRef();
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
        <>
            <Tooltip title={'Change icon'}>
                {/*<SomeContent/>*/}
                <TooltipIconButton
                    onClick={handleClickOpen}
                    iconName={selectedIcon.name}
                    iconColor={selectedIcon.color != null ? selectedIcon.color : getButtonColor(theme)}
                    iconSvg={selectedIcon.svg}/>
            </Tooltip>
            <SimpleDialog initIcon={selectedIcon} open={open} onClose={handleClose}/>
        </>
    );
}
