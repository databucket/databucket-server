import React from 'react';
import {parseCustomSvg} from "./SvgHelper";
import {Icon, SvgIcon} from "@mui/material";
import {getIconColor} from "../../utils/MaterialTableHelper";
import {styled, useTheme} from "@mui/material/styles";

const InnerStyledIcon = styled(Icon)(({theme}) => ({
    marginRight: theme.spacing(1)
}));

export default function StyledIcon({onClick, iconName, iconColor, iconSvg, ...props}) {
    const theme = useTheme();

    if (!iconSvg) {
        return (<InnerStyledIcon sx={{color: iconColor}}>{iconName}</InnerStyledIcon>);
    }
    return (
        <SvgIcon {...props}>
            {parseCustomSvg(iconSvg, getIconColor(theme.palette.mode, iconColor))}
        </SvgIcon>
    );
}
