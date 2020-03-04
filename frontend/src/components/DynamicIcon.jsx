import React from 'react';
import ErrorIcon from '@material-ui/icons/Error';
import * as Icons from '@material-ui/icons';

export default function DynamicIcon({ iconName, color}) {
    const Icon = Icons[iconName];
    return Icon ? color != null ? <Icon color={color}/> : <Icon /> : <ErrorIcon color="error"/>;
} 