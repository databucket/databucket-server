import React, {useContext, useState} from 'react';
import {
    Button,
    Checkbox,
    FormControl,
    FormControlLabel,
    IconButton,
    InputLabel,
    Menu,
    MenuItem,
    Select,
    Slider,
    styled,
    Tooltip,
    Typography
} from '@mui/material';
import {getUsername, hasAdminRole} from "../../utils/ConfigurationStorage";
import AccessContext from "../../context/access/AccessContext";

const PREFIX = 'ReserveDataDialog';

const classes = {
    root: `${PREFIX}-root`,
    reserveButton: `${PREFIX}-reserveButton`,
    content: `${PREFIX}-content`,
    button: `${PREFIX}-button`,
    formControl: `${PREFIX}-formControl`,
    selectEmpty: `${PREFIX}-selectEmpty`
};

const Root = styled('div')((
    {
        theme
    }
) => ({
    [`&.${classes.root}`]: {
        flexGrow: 1
    },

    [`& .${classes.reserveButton}`]: {
        marginLeft: '10px',
        padding: theme.spacing(1)
    },

    [`& .${classes.content}`]: {
        display: "flex",
        flexDirection: "column",
        justifyContent: "flex-start",
        alignItems: "flex-start",
        padding: "15px",
        margin: theme.spacing(1),
    },

    [`& .${classes.button}`]: {
        marginTop: "13px"
    },

    [`& .${classes.formControl}`]: {
        margin: theme.spacing(1),
        minWidth: 150,
    },

    [`& .${classes.selectEmpty}`]: {
        marginTop: theme.spacing(2),
    }
}));

// ReserveDataDialog.propTypes = {
//     onReserve: PropTypes.func.isRequired
// }

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

    const onChangeUser = (user) => {
        setState({...state, username: user.username});
    }

    const handleReserve = () => {
        props.onReserve(state);
        handleClose();
    }

    return (
        <Root className={classes.root}>
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
                id="menu-appbar"
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
                <div className={classes.content}>
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
                    <FormControl className={classes.formControl}>
                        <InputLabel id="user-select-label">Target owner</InputLabel>
                        <Select
                            labelId="user-select-label"
                            id="user-select"
                            value={state.username}
                            // onChange={onChangeUser}
                        >
                            {users.map(user => (
                                <MenuItem key={user.id} value={user.username} onClick={() => onChangeUser(user)}>
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
                </div>
            </Menu>
        </Root>
    );
}