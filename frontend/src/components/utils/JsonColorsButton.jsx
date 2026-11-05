import React, {useState} from 'react';
import {Box, IconButton, ListItemIcon, ListItemText, Menu, MenuItem, Tooltip, useTheme} from '@mui/material';
import {Check as CheckIcon, Palette as PaletteIcon} from '@mui/icons-material';
import {getJsonPalette, JSON_PALETTES, setJsonPalette} from '../../utils/JsonColors';

// a piece of JSON in the colors of a palette
const Sample = ({palette}) => (
    <Box component="span" sx={{
        fontFamily: 'monospace', fontSize: '0.85rem', px: 1, py: 0.25, borderRadius: 1,
        backgroundColor: palette.background, color: palette.text, whiteSpace: 'nowrap'
    }}>
        <span style={{color: palette.key}}>"id"</span>: <span style={{color: palette.number}}>12</span>,{' '}
        <span style={{color: palette.key}}>"name"</span>: <span style={{color: palette.string}}>"Anna"</span>,{' '}
        <span style={{color: palette.key}}>"vip"</span>: <span style={{color: palette.literal}}>true</span>
    </Box>
);

// Chooses the colors of JSON for the current theme (light or dark): the JSON editor of the data details and the JSON
// in the data history
export default function JsonColorsButton({size = 'large', style}) {
    const theme = useTheme();
    const mode = theme.palette.mode;
    const [anchor, setAnchor] = useState(null);
    const [current, setCurrent] = useState(null);

    const open = (event) => {
        setCurrent(getJsonPalette(mode).name);
        setAnchor(event.currentTarget);
    };

    const choose = (name) => {
        setJsonPalette(mode, name);
        setAnchor(null);
    };

    return (
        <>
            <Tooltip title="JSON colors">
                <IconButton color="inherit" onClick={open} size={size} style={style}>
                    <PaletteIcon/>
                </IconButton>
            </Tooltip>
            <Menu anchorEl={anchor} open={anchor != null} onClose={() => setAnchor(null)}>
                {JSON_PALETTES[mode === 'dark' ? 'dark' : 'light'].map(palette => (
                    <MenuItem key={palette.name} selected={palette.name === current} onClick={() => choose(palette.name)}>
                        <ListItemIcon>{palette.name === current && <CheckIcon fontSize="small"/>}</ListItemIcon>
                        <ListItemText sx={{mr: 2}}>{palette.name}</ListItemText>
                        <Sample palette={palette}/>
                    </MenuItem>
                ))}
            </Menu>
        </>
    );
}
