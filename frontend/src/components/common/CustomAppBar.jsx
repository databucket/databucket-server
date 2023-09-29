import {AppBar, lighten, styled, Tab} from "@mui/material";

export const drawerClosedWidth = 73;
export const drawerWidth = 260;

export const CustomAppBar = styled(AppBar, {
    shouldForwardProp: (prop) => prop !== 'open',
})(({ theme, open }) => ({
    background: theme.common.toolbar.backgroundColor,
    zIndex: theme.zIndex.drawer + 1,
    transition: theme.transitions.create(['margin', 'width'], {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
    }),
    ...(open && {
        width: `calc(100% - ${drawerWidth}px)`,
        marginLeft: drawerWidth,
        transition: theme.transitions.create(['margin', 'width'], {
            easing: theme.transitions.easing.easeOut,
            duration: theme.transitions.duration.enteringScreen,
        }),
    }),
}));

export const CustomTab = styled(Tab)(({theme}) => ({

    "&:hover": {
        opacity: 0.9,
        color: theme.palette.primary.contrastText,
        backgroundColor: lighten(theme.common.toolbar.backgroundColor, 0.05)
    },

    '&.Mui-selected': {
        color: theme.palette.primary.contrastText,
    },

}));
