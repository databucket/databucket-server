import React from 'react';
import {parseCustomSvg} from "./SvgHelper";
import PropTypes from "prop-types";
import {SvgIcon} from "@material-ui/core";
import {getIconColor} from "../../utils/MaterialTableHelper";

StyledIcon.propTypes = {
    iconName: PropTypes.string.isRequired,
    iconColor: PropTypes.string.isRequired,
    iconSvg: PropTypes.string.isRequired,
    themeType: PropTypes.string.isRequired
};

export default function StyledIcon(props) {
    if (props.iconSvg != null)
        return (
            <SvgIcon>
                {parseCustomSvg(props.iconSvg, getIconColor(props.themeType, props.iconColor))}
            </SvgIcon>
        );
    else
        return (
            <span
                style={{color: getIconColor(props.themeType, props.iconColor)}}
                className="material-icons"
            >
                {props.iconName}
            </span>
        );
}