import {createMuiTheme} from "@material-ui/core/styles";

export const LightTheme = createMuiTheme({
    palette: {
        type: 'light',
        primary: {
            main: '#0d47a1'
        },
        secondary: {
            main: '#ff9800'
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
            primary: '#C8C8C8'
        }
    },
});

export const getAppBarBackgroundColor = () => {
    return '#0d47a1';
}