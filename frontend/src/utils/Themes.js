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
            paper: '#eeeeee'
        },
        primary: {
            main: '#0d47a1',
            contrastText: '#eeeee4'
        },
        secondary: {
            main: '#eb9414'
        },
        text: {
            primary: '#4e4e4e'
            // secondary: '#585858'
        }
    },
    components: {
        MuiCheckbox: {
            defaultProps: {
                color: 'secondary'
            }
        },
        MuiSelect: {
            defaultProps: {
                variant: 'standard'
            },
            styleOverrides: {
                root: {
                    '&:before': {
                        borderBottom: `1px solid #4e4e4e`
                    },
                    '&:after': {
                        borderBottom: `2px solid #4e4e4e`
                    },
                    '&:hover:not(.Mui-disabled):before': {
                        borderBottom: `2px solid #4e4e4e`
                    }
                }
            }
        },
        MuiInput: {
            styleOverrides: {
                root: {
                    '&:before': {
                        borderBottom: `1px solid #4e4e4e`
                    },
                    '&:after': {
                        borderBottom: `2px solid #4e4e4e`
                    },
                    '&:hover:not(.Mui-disabled):before': {
                        borderBottom: `2px solid #4e4e4e`
                    }
                }
            }
        },
        MuiOutlinedInput: {
            defaultProps: {
                color: 'secondary',
                size: 'small'
            }
        }
    }
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
            paper: '#43453e'
        },
        primary: {
            main: '#357eef',
            contrastText: '#ebf2fd'
        },
        secondary: {
            main: '#ff9800'
        },
        text: {
            primary: '#d6d7d2'
        }
    },
    components: {
        MuiPaper: {
            styleOverrides: {
                root: {
                    backgroundImage: 'unset', // remove background-image
                },
            },
        },
        MuiCheckbox: {
            defaultProps: {
                color: 'secondary'
            }
        },
        MuiSelect: {
            defaultProps: {
                variant: 'standard'
            },
            styleOverrides: {
                root: {
                    '&:before': {
                        borderBottom: `1px solid #d6d7d2`
                    },
                    '&:after': {
                        borderBottom: `2px solid #d6d7d2`
                    },
                    '&:hover:not(.Mui-disabled):before': {
                        borderBottom: `2px solid #d6d7d2`
                    }
                }
            }
        },
        MuiInput: {
            styleOverrides: {
                root: {
                    '&:before': {
                        borderBottom: `1px solid #d6d7d2`
                    },
                    '&:after': {
                        borderBottom: `2px solid #d6d7d2`
                    },
                    '&:hover:not(.Mui-disabled):before': {
                        borderBottom: `2px solid #d6d7d2`
                    }
                }
            }
        },
        MuiOutlinedInput: {
            defaultProps: {
                color: 'secondary',
                size: 'small'
            }
        }
    },
};
