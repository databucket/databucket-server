import React from 'react';
import IconButton from "@mui/material/IconButton";
import { useTheme } from "@mui/material/styles";
import makeStyles from '@mui/styles/makeStyles';
import {parseCustomSvg} from "./SvgHelper";
import PropTypes from "prop-types";
import {getIconColor} from "../../utils/MaterialTableHelper";

const useStyles = makeStyles(theme => ({
    customStyles: {
        color: (props) => props.iconColor
    }
}));

StyledIconButton.propTypes = {
    iconName: PropTypes.string.isRequired,
    iconColor: PropTypes.string.isRequired,
    iconSvg: PropTypes.string.isRequired,
    onClick: PropTypes.func
};

export default function StyledIconButton(props) {
    const classes = useStyles(props);
    const theme = useTheme();

    if (props.iconSvg != null)
        return (
            <IconButton onClick={props.onClick} className={classes.customStyles} size="large">
                {parseCustomSvg(props.iconSvg, getIconColor(theme.palette.mode, props.iconColor))}
            </IconButton>
        );
    else
        return (
            <IconButton onClick={props.onClick} className={classes.customStyles} size="large">
                <span
                    style={{color: getIconColor(theme.palette.mode, props.iconColor)}}
                    className="material-icons"
                >
                {props.iconName}
                </span>
            </IconButton>
        );
}