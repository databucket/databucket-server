export function getTheme(name) {
    if (name === 'dark') {
        return DarkTheme;
    } else {
        return LightTheme;
    }
}

export const LightTheme = {
    common: {
        toolbar: {
            backgroundColor: '#0d47a1'
        }
    },
    palette: {
        mode: 'light',
        background: {
            default: '#f7f7f7',
            paper: '#eeeeee',
        },
        primary: {
            main: '#0d47a1',
            contrastText: '#eeeee4'
        },
        secondary: {
            main: '#ff9800'
        },
        text: {
            primary: '#4e4e4e'
            // secondary: '#585858'
        }
    },
};

export const DarkTheme = {
    common: {
        toolbar: {
            backgroundColor: '#0d47a1'
        }
    },
    palette: {
        mode: 'dark',
        background: {
            default: '#2f3129',
            paper: '#43453e',
        },
        primary: {
            main: '#357eef',
            contrastText: '#ebf2fd',
        },
        secondary: {
            main: '#ff9800'
        },
        text: {
            primary: '#d6d7d2',
            // secondary: '#c2c4be'
        }
    },
};
