import React from 'react';
import StyledIcon from "./StyledIcon";

export default function TableDynamicIcon({icon}) {
    if (icon != null)
        return (<StyledIcon iconName={icon.name} iconColor={icon.color} iconSvg={icon.svg}/>);
    else
        return <div/>
}
