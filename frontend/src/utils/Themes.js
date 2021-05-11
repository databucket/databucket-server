import {createMuiTheme} from "@material-ui/core/styles";

export function getTheme(name) {
    switch (name) {
        case 'dark':
            return DarkTheme;
        default:
            return LightTheme;
    }
}

export const LightTheme = createMuiTheme({
    palette: {
        type: 'light',
        primary: {
            main: '#0d47a1',
            contrastText: '#ebf2fd',
        },
        secondary: {
            main: '#ff9800'
        },
        text: {
            primary: '#4A4A4A',
            // secondary: '#e31f00' # MaterialTable buttons
        }
    },
});

export const DarkTheme = createMuiTheme({
    palette: {
        type: 'dark',
        primary: {
            main: '#357eef',
            contrastText: '#ebf2fd',
        },
        secondary: {
            main: '#ff9800'
        },
        text: {
            primary: '#d5d5d5'
        }
    },
});

export const getAppBarBackgroundColor = () => {
    return '#0d47a1';
}