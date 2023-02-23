import React from 'react';
import {parseCustomSvg} from "./SvgHelper";
import {Icon, SvgIcon} from "@mui/material";
import {getIconColor} from "../../utils/MaterialTableHelper";
import {styled, useTheme} from "@mui/material/styles";

const InnerStyledIcon = styled(Icon)(({theme}) => ({
    color: (props) => getIconColor(theme.palette.mode, props.iconColor),
    marginRight: theme.spacing(1)
}));

export default function StyledIcon({onClick, iconName, iconColor, iconSvg, ...props}) {
    const theme = useTheme();

    if (iconSvg != null)
        return (
            <SvgIcon {...props}>
                {parseCustomSvg(iconSvg, getIconColor(theme.palette.mode, iconColor))}
            </SvgIcon>
        );
    else
        return (
            <InnerStyledIcon>{iconName}</InnerStyledIcon>
        );
}
