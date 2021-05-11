import React from 'react';
import {Icon} from "@material-ui/core";

export default function TableDynamicIcon({iconName}) {
    if (iconName != null)
        return (
            <Icon style={{textAlign: "center", verticalAlign: "middle"}}>
                <span className="material-icons">{iconName}</span>
            </Icon>
        );
    else
        return <div/>
}