import React from 'react';
import IconButton from "@material-ui/core/IconButton";
import {makeStyles, useTheme} from "@material-ui/core/styles";
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
            <IconButton onClick={props.onClick} className={classes.customStyles}>
                {parseCustomSvg(props.iconSvg, getIconColor(theme.palette.type, props.iconColor))}
            </IconButton>
        );
    else
        return (
            <IconButton onClick={props.onClick} className={classes.customStyles}>
                <span
                    style={{color: getIconColor(theme.palette.type, props.iconColor)}}
                    className="material-icons"
                >
                {props.iconName}
                </span>
            </IconButton>
        );
}