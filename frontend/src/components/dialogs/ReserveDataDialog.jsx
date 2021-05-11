import React, {useContext, useState} from 'react';
import {makeStyles} from '@material-ui/core/styles';
import IconButton from '@material-ui/core/IconButton';
import Menu from '@material-ui/core/Menu';
import {Checkbox, FormControlLabel, InputLabel, MenuItem, Select, Slider, Tooltip} from "@material-ui/core";
import Typography from "@material-ui/core/Typography";
import {getUsername, hasAdminRole} from "../../utils/ConfigurationStorage";
import FormControl from "@material-ui/core/FormControl";
import Button from "@material-ui/core/Button";
import UsersContext from "../../context/users/UsersContext";

const useStyles = makeStyles((theme) => ({
    root: {
        flexGrow: 1
    },
    reserveButton: {
        marginLeft: '10px',
        padding: theme.spacing(1)
    },
    content: {
        display: "flex",
        flexDirection: "column",
        justifyContent: "flex-start",
        alignItems: "flex-start",
        padding: "15px",
        margin: theme.spacing(1),
    },
    button: {
        marginTop: "13px"
    },
    formControl: {
        margin: theme.spacing(1),
        minWidth: 150,
    },
    selectEmpty: {
        marginTop: theme.spacing(2),
    },
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

    const classes = useStyles();
    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);
    const usersContext = useContext(UsersContext);
    const {users} = usersContext;
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
        <div className={classes.root}>
            <Tooltip title={'Reserve data'}>
                <IconButton onClick={handleMenu} color={'inherit'} className={classes.reserveButton}>
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
        </div>
    );
}