import React from 'react';
import {styled, useTheme} from '@mui/material/styles';
import IconButton from "@mui/material/IconButton";
import {parseCustomSvg} from "./SvgHelper";
import PropTypes from "prop-types";
import {getIconColor} from "../../utils/MaterialTableHelper";

const PREFIX = 'StyledIconButton';

const classes = {
    customStyles: `${PREFIX}-customStyles`
};

const TheStyledIconButton = styled(IconButton)((
    {
        theme
    }
) => ({
    [`&.${classes.customStyles}`]: {
        color: (props) => props.iconColor
    }
}));

TheStyledIconButton.propTypes = {
    iconName: PropTypes.string.isRequired,
    iconColor: PropTypes.string.isRequired,
    iconSvg: PropTypes.string.isRequired,
    onClick: PropTypes.func
};

export default function StyledIconButton(props) {
    const theme = useTheme();

    if (props.iconSvg != null)
        return (
            <StyledIconButton onClick={props.onClick} className={classes.customStyles} size="large">
                {parseCustomSvg(props.iconSvg, getIconColor(theme.palette.mode, props.iconColor))}
            </StyledIconButton>
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
