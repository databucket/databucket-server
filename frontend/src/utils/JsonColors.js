// Colors of JSON: three palettes for the light theme and three for the dark one. The user picks one per theme in the
// data details (JsonColorsButton); it colors the JSON editor of the data details (through CSS variables, see
// components/styles/json-editor-styles.css) and the JSON in the data history (JsonHighlight.jsx).

export const JSON_PALETTES = {
    light: [
        {name: 'VS Code', background: '#f7f7f7', text: '#000000', key: '#0451a5', string: '#a31515', number: '#098658', literal: '#0000ff'},
        {name: 'GitHub', background: '#ffffff', text: '#24292e', key: '#005cc5', string: '#032f62', number: '#e36209', literal: '#d73a49'},
        {name: 'Solarized', background: '#fdf6e3', text: '#657b83', key: '#268bd2', string: '#2aa198', number: '#d33682', literal: '#b58900'}
    ],
    dark: [
        {name: 'VS Code', background: '#393b34', text: '#f8f8f2', key: '#9cdcfe', string: '#ce9178', number: '#b5cea8', literal: '#569cd6'},
        {name: 'Monokai', background: '#272822', text: '#f8f8f2', key: '#66d9ef', string: '#e6db74', number: '#ae81ff', literal: '#f92672'},
        {name: 'Dracula', background: '#282a36', text: '#f8f8f2', key: '#8be9fd', string: '#f1fa8c', number: '#bd93f9', literal: '#ff79c6'}
    ]
};

const storageKey = mode => 'json-colors-' + mode;
const palettesOf = mode => JSON_PALETTES[mode === 'dark' ? 'dark' : 'light'];
// the choice made in this page, also when the browser keeps no storage
const chosen = {};

// the chosen palette of the theme (light or dark), the first one when none is chosen
export const getJsonPalette = (mode) => {
    const palettes = palettesOf(mode);
    let name = chosen[mode] ?? null;
    try {
        name = name ?? localStorage.getItem(storageKey(mode));
    } catch (e) {
        // no storage: the default palette
    }
    return palettes.find(palette => palette.name === name) ?? palettes[0];
}

// the colors of the theme as CSS variables on <html>, used by the JSON editor
const applyPalette = (palette) => {
    const style = document.documentElement.style;
    ['background', 'text', 'key', 'string', 'number', 'literal']
        .forEach(part => style.setProperty('--json-' + part, palette[part]));
}

export const applyJsonColors = (mode) => applyPalette(getJsonPalette(mode));

export const setJsonPalette = (mode, name) => {
    chosen[mode] = name;
    try {
        localStorage.setItem(storageKey(mode), name);
    } catch (e) {
        // no storage: the choice holds until the page is reloaded
    }
    applyJsonColors(mode);
}
