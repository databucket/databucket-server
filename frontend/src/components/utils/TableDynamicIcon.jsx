import React from 'react';
import StyledIcon from "./StyledIcon";
import {useTheme} from "@mui/material/styles";

export default function TableDynamicIcon({icon}) {

    const theme = useTheme();

    if (icon != null)
        return (<StyledIcon iconName={icon.name} iconColor={icon.color} iconSvg={icon.svg} themeType={theme.palette.mode}/>);
    else
        return <div/>
}