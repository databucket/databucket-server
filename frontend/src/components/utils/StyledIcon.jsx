import React from 'react';
import {parseCustomSvg} from "./SvgHelper";
import PropTypes from "prop-types";
import {SvgIcon} from "@material-ui/core";

StyledIcon.propTypes = {
    iconName: PropTypes.string.isRequired,
    iconColor: PropTypes.string.isRequired,
    iconSvg: PropTypes.string.isRequired
};

export default function StyledIcon(props) {
    if (props.iconSvg != null)
        return (
            // <SvgIcon style={{textAlign: "center", verticalAlign: "middle"}}>
            <SvgIcon>
                {parseCustomSvg(props.iconSvg, props.iconColor)}
            </SvgIcon>
        );
    else
        return (<span style={{color: props.iconColor}} className="material-icons">{props.iconName}</span>);
}