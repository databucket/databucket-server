import React from 'react';
import {parseCustomSvg} from "./SvgHelper";
import {Icon, styled, SvgIcon, useTheme} from "@mui/material";
import {getIconColor} from "../../utils/MaterialTableHelper";

// in the middle of the line: on the baseline a 24px icon makes a line of text (e.g. a table cell) taller,
// so rows with an icon were higher than the others
const InnerStyledIcon = styled(Icon)(({theme}) => ({
    marginRight: theme.spacing(1),
    verticalAlign: 'middle'
}));

export default function StyledIcon({onClick, iconName, iconColor, iconSvg, ...props}) {
    const theme = useTheme();

    if (!iconSvg) {
        return (<InnerStyledIcon sx={{color: iconColor}}>{iconName}</InnerStyledIcon>);
    }
    return (
        <SvgIcon {...props} sx={{verticalAlign: 'middle', ...props.sx}}>
            {parseCustomSvg(iconSvg, getIconColor(theme.palette.mode, iconColor))}
        </SvgIcon>
    );
}
