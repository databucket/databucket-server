import React from 'react';

export default function DynamicIcon({iconName, color}) {
    if (color != null)
        return (<span className="material-icons" color={color}>{iconName}</span>);
    else
        return (<span className="material-icons">{iconName}</span>);
}