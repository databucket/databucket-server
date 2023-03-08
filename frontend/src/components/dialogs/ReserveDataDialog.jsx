import React, {useContext, useState} from 'react';
import {styled} from '@mui/material/styles';
import IconButton from '@mui/material/IconButton';
import Menu from '@mui/material/Menu';
import {Box, Checkbox, FormControlLabel, InputLabel, MenuItem, Select, Slider, Tooltip} from "@mui/material";
import Typography from "@mui/material/Typography";
import {getUsername, hasAdminRole} from "../../utils/ConfigurationStorage";
import FormControl from "@mui/material/FormControl";
import Button from "@mui/material/Button";
import AccessContext from "../../context/access/AccessContext";

const PREFIX = 'ReserveDataDialog';

const classes = {
    root: `${PREFIX}-root`,
    reserveButton: `${PREFIX}-reserveButton`,
    button: `${PREFIX}-button`,
    formControl: `${PREFIX}-formControl`,
};

const Root = styled(Box)(({theme}) => ({
    [`&.${classes.root}`]: {
        flexGrow: 1
    },

    [`& .${classes.reserveButton}`]: {
        marginLeft: '10px',
        padding: theme.spacing(1)
    },

    [`& .${classes.button}`]: {
        marginTop: "13px"
    },

}));

const marks = [
    {
        value: 1,
        label: '1',
    },
    {
        value: 5,
        label: '5',
    },
    {
        value: 10,
        label: '10',
    },
    {
        value: 15,
        label: '15',
    },
    {
        value: 20,
        label: '20',
    }
];

const Content = styled('div')(({theme}) => ({
    display: "flex",
    flexDirection: "column",
    justifyContent: "flex-start",
    alignItems: "flex-start",
    padding: "15px",
    margin: theme.spacing(1),

    [`& .${classes.formControl}`]: {
        margin: theme.spacing(1),
        minWidth: 150,
    },
}));

export default function ReserveDataDialog(props) {

    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);
    const accessContext = useContext(AccessContext);
    const {users} = accessContext;
    const [state, setState] = useState({random: false, number: 1, username: getUsername()});

    const handleMenu = (event) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleChangeRandom = (event, newValue) => {
        setState({...state, random: newValue});
    };

    const handleChangeNumber = (event, newValue) => {
        setState({...state, number: newValue});
    }

    const onChangeUser = (event) => {
        const user = event.target.value;
        setState({...state, username: user});
    }

    const handleReserve = () => {
        props.onReserve(state);
        handleClose();
    }

    return (
        <Root>
            <Tooltip title={'Reserve data'}>
                <IconButton
                    onClick={handleMenu}
                    color={'inherit'}
                    className={classes.reserveButton}
                    size="large">
                    <span className="material-icons">add_task</span>
                </IconButton>
            </Tooltip>
            <Menu
                id="reserve-data-menu"
                anchorEl={anchorEl}
                anchorOrigin={{
                    vertical: 'top',
                    horizontal: 'right',
                }}
                keepMounted
                transformOrigin={{
                    vertical: 'top',
                    horizontal: 'right',
                }}
                open={open}
                onClose={handleClose}
            >
                <Content>
                    <FormControl className={classes.formControl}>
                        <Typography gutterBottom>
                            Number of data rows
                        </Typography>
                        <Slider
                            value={state.number}
                            // defaultValue={state.number}
                            aria-labelledby="discrete-slider-steps"
                            step={1}
                            marks={marks}
                            min={1}
                            max={20}
                            valueLabelDisplay="auto"
                            onChange={handleChangeNumber}
                        />
                    </FormControl>
                    {hasAdminRole() &&
                        <FormControl fullWidth>
                            <InputLabel id="user-select-label">Target owner</InputLabel>
                            <Select
                                labelId="user-select-label"
                                id="user-select"
                                value={state.username}
                                label="Target owner"
                                onChange={onChangeUser}
                            >
                                {users.map(user => (
                                    <MenuItem key={user.id}
                                              value={user.username}>
                                        {user.username}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    }
                    <FormControlLabel
                        control={<Checkbox checked={state.random} onChange={handleChangeRandom} name="checkRandom"/>}
                        label="Random data"
                        className={classes.formControl}
                    />
                    <FormControl className={classes.formControl}>
                        <Button variant="contained" color="secondary" id="reserveButton" onClick={handleReserve}>
                            Reserve
                        </Button>
                    </FormControl>
                </Content>
            </Menu>
        </Root>
    );
}
