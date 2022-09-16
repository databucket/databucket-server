import React from 'react';
import IconButton from "@material-ui/core/IconButton";
import {makeStyles} from "@material-ui/core/styles";
import {parseCustomSvg} from "./SvgHelper";
import PropTypes from "prop-types";

const useStyles = makeStyles(theme => ({
    button: {
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
    const classes = useStyles(props);

    if (props.iconSvg != null)
        return (
            <IconButton disabled onClick={props.onClick} className={classes.button}>
                {parseCustomSvg(props.iconSvg, props.iconColor)}
            </IconButton>
        );
    else
        return (
            <IconButton disabled onClick={props.onClick} className={classes.button}>
                <span className="material-icons">{props.iconName}</span>
            </IconButton>
        );
}