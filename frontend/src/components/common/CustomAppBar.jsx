import {lighten, styled} from "@mui/material/styles";
import AppBar from "@mui/material/AppBar";
import Tab from "@mui/material/Tab";

export const drawerWidth = 260;

export const CustomAppBar = styled(AppBar, {
    shouldForwardProp: (prop) => prop !== 'open',
})(({ theme, open }) => ({
    background: theme.common.toolbar.backgroundColor,
    transition: theme.transitions.create(['margin', 'width'], {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
    }),
    ...(open && {
        width: `calc(100% - ${drawerWidth}px)`,
        marginLeft: `${drawerWidth}px`,
        transition: theme.transitions.create(['margin', 'width'], {
            easing: theme.transitions.easing.easeOut,
            duration: theme.transitions.duration.enteringScreen,
        }),
    }),
}));

export const CustomTab = styled(Tab)(({theme}) => ({

    "&:hover": {
        backgroundColor: lighten(theme.common.toolbar.backgroundColor, 0.05)
    },

    '&.Mui-selected': {
        color: theme.palette.primary.contrastText,
    },

}));
