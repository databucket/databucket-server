import React from 'react';
import { styled } from '@mui/material/styles';
import IconButton from "@mui/material/IconButton";
import { useTheme } from "@mui/material/styles";
import {parseCustomSvg} from "./SvgHelper";
import PropTypes from "prop-types";

const PREFIX = 'StyledIconButtonTab';

const classes = {
    button: `${PREFIX}-button`
};

const StyledIconButton = styled(IconButton)((
    {
        theme
    }
) => ({
    [`&.${classes.button}`]: {
        color: (props) => props.iconColor != null ? props.iconColor : theme.palette.primary.contrastText,
        "&:disabled": {
            color: (props) => props.iconColor != null ? props.iconColor : theme.palette.primary.contrastText
        }
    }
}));

StyledIconButtonTab.propTypes = {
    iconName: PropTypes.string.isRequired,
    iconColor: PropTypes.string.isRequired,
    iconSvg: PropTypes.string.isRequired,
    onClick: PropTypes.func
};

export default function StyledIconButtonTab(props) {

    const theme = useTheme();

    if (props.iconSvg != null)
        return (
            <StyledIconButton disabled onClick={props.onClick} className={classes.button} size="large">
                {parseCustomSvg(props.iconSvg, props.iconColor != null ? props.iconColor : theme.palette.primary.contrastText)}
            </StyledIconButton>
        );
    else
        return (
            <IconButton disabled onClick={props.onClick} className={classes.button} size="large">
                <span className="material-icons">{props.iconName}</span>
            </IconButton>
        );
}