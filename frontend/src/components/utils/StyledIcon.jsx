import React from 'react';
import {parseCustomSvg} from "./SvgHelper";
import {SvgIcon} from "@mui/material";
import {getIconColor} from "../../utils/MaterialTableHelper";

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
                style={{color: getIconColor(props.themeType, props.iconColor).main}}
                className="material-icons"
            >
                {props.iconName}
            </span>
        );
}
