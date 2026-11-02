import React from "react";
import {
    Checkbox,
    FormControl,
    FormControlLabel,
    FormGroup,
    MenuItem,
    Radio,
    RadioGroup,
    Select,
    styled,
    TextField
} from '@mui/material';
import PropTypes from "prop-types";
import ActionPropertiesTable from "./ActionPropertiesTable";
import {getUsername} from "../../utils/ConfigurationStorage";

// the given fields of an object that are set
const pick = (object, keys) => Object.fromEntries(keys.filter(key => object?.[key] != null).map(key => [key, object[key]]));

const PREFIX = 'TaskActions';

const classes = {
    settingsContainer: `${PREFIX}-settingsContainer`,
    propertiesContainer: `${PREFIX}-propertiesContainer`
};

const Root = styled('div')(() => ({
    [`& .${classes.settingsContainer}`]: {
        height: '27%',
        marginLeft: '20px',
        marginTop: '10px'
    },

    [`& .${classes.propertiesContainer}`]: {
        height: '72%'
    }
}));

TaskActions.propTypes = {
    actions: PropTypes.object,
    properties: PropTypes.array,
    tags: PropTypes.array,
    onChange: PropTypes.func.isRequired,
    pageSize: PropTypes.number,
    // remove, modify and clear history (false: only reserve, when the view has reservation but no tasks)
    taskTypes: PropTypes.bool,
    // reserve data: in the task execution of a view with reservation (not in saved tasks)
    reserveType: PropTypes.bool,
    // clear history: false in the task execution of a bucket that keeps no history
    historyType: PropTypes.bool,
    // the users an admin can reserve for (target owner); null: only for the current user
    owners: PropTypes.array
}

// the most data rows reserved at once
export const MAX_RESERVATION = 1000;

// the settings of reserving data: free rows matching the rules, for the current user or, by an admin, another one
export const reserveActions = (username) => ({type: 'reserve', number: 1, random: false, username, properties: []});

export const isReserveValid = (actions) => Number.isInteger(actions.number) && actions.number >= 1
    && actions.number <= MAX_RESERVATION;

export default function TaskActions(props) {


    const actionsPropertiesContentRef = React.useRef(null);
    const taskTypes = props.taskTypes !== false;

    const handleActionTypeChange = (event) => {
        if (event.target.value === 'reserve')
            props.onChange({...reserveActions(getUsername()), ...pick(props.actions, ['number', 'random', 'username'])});
        else
            props.onChange({...props.actions, type: event.target.value});
    }

    const handleActionSetTag = (event) => {
        props.onChange({...props.actions, setTag: event.target.checked, tagId: props.tags[0].id});
    }

    const handleActionSetReserved = (event) => {
        if (event.target.checked === true && props.actions.reserved == null)
            props.onChange({...props.actions, setReserved: event.target.checked, reserved: true});
        else
            props.onChange({...props.actions, setReserved: event.target.checked});
    }

    const handleActionReserveChange = (event) => {
        props.onChange({...props.actions, reserved: (event.target.value === 'true')});
    }

    const handleTagChange = (event) => {
        props.onChange({...props.actions, tagId: event.target.value});
    }

    const onActionPropertiesChange = (updatedProperties) => {
        props.onChange({...props.actions, properties: updatedProperties});
    }

    return (
        <Root style={{height: '95%'}}>
            <div className={classes.settingsContainer}>
                <FormControl component="fieldset">
                    <RadioGroup
                        row
                        value={props.actions.type || ''}
                        onChange={handleActionTypeChange}
                    >
                        {taskTypes && <FormControlLabel
                            value="remove"
                            control={<Radio color="secondary" />}
                            label="Remove data"
                        />}
                        {taskTypes && <FormControlLabel
                            value="modify"
                            control={<Radio color="secondary" />}
                            label="Modify data"
                        />}
                        {/* a saved task that clears the history still shows what it does */}
                        {taskTypes && (props.historyType !== false || props.actions.type === 'clear history') && <FormControlLabel
                            value="clear history"
                            control={<Radio color="secondary" />}
                            label="Clear history"
                        />}
                        {props.reserveType && <FormControlLabel
                            value="reserve"
                            control={<Radio color="secondary" />}
                            label="Reserve data"
                        />}
                    </RadioGroup>
                    {props.actions.type === 'reserve' &&
                    <div style={{display: 'flex', alignItems: 'flex-start', gap: 32, marginTop: 8}}>
                        <TextField
                            label="Number of data rows"
                            type="number"
                            variant="standard"
                            color="secondary"
                            value={Number.isNaN(props.actions.number) ? '' : props.actions.number}
                            onChange={e => props.onChange({...props.actions, number: parseInt(e.target.value, 10)})}
                            inputProps={{min: 1, max: MAX_RESERVATION, step: 1}}
                            error={!isReserveValid(props.actions)}
                            helperText={isReserveValid(props.actions) ? ' ' : `From 1 to ${MAX_RESERVATION}`}
                            sx={{width: 160, flexShrink: 0}}
                        />
                        {props.owners != null &&
                        <TextField
                            select
                            label="Target owner"
                            variant="standard"
                            color="secondary"
                            value={props.actions.username}
                            onChange={e => props.onChange({...props.actions, username: e.target.value})}
                            sx={{minWidth: 160, flexShrink: 0}}
                        >
                            {props.owners.map(user =>
                                <MenuItem key={user.id} value={user.username}>{user.username}</MenuItem>)}
                        </TextField>
                        }
                        <FormControlLabel
                            label="Random data rows"
                            sx={{mt: 1.5}}
                            control={<Checkbox checked={props.actions.random || false} color="secondary"
                                               onChange={e => props.onChange({...props.actions, random: e.target.checked})}/>}
                        />
                    </div>
                    }
                    {props.actions.type === 'modify' &&
                    <div>
                        {props.tags != null && props.tags.length > 0 &&
                        <FormGroup row>
                            <FormControlLabel
                                label="Set tag"
                                control={<Checkbox checked={props.actions.setTag || false} onChange={handleActionSetTag} color="secondary"/>}
                            />
                            {props.actions.setTag === true &&
                            <FormControlLabel
                                label=''
                                labelPlacement="start"
                                control={
                                    <Select
                                        id="tag-select"
                                        onChange={handleTagChange}
                                        value={props.actions.tagId}
                                        color="secondary"
                                        variant="standard"
                                    >
                                        {props.tags.map(tag => (
                                            <MenuItem key={tag.id} value={tag.id}>
                                                {tag.name}
                                            </MenuItem>
                                        ))}
                                    </Select>
                                }
                            />
                            }
                        </FormGroup>
                        }
                        <FormGroup row>
                            <FormControlLabel
                                label="Set reserved"
                                control={<Checkbox checked={props.actions.setReserved || false} onChange={handleActionSetReserved} color="secondary"/>}
                            />
                            {props.actions.setReserved === true &&
                            <RadioGroup row value={props.actions.reserved || false} onChange={handleActionReserveChange}>
                                <FormControlLabel
                                    value={true}
                                    control={<Radio color="secondary"/>}
                                    label="True"
                                />
                                <FormControlLabel
                                    value={false}
                                    control={<Radio color="secondary"/>}
                                    label="False"
                                />
                            </RadioGroup>
                            }
                        </FormGroup>
                    </div>
                    }
                </FormControl>
            </div>
            {props.actions.type === 'modify' &&
            <div ref={actionsPropertiesContentRef} className={classes.propertiesContainer}>
                <ActionPropertiesTable
                    data={props.actions.properties}
                    properties={props.properties}
                    onChange={onActionPropertiesChange}
                    pageSize={props.pageSize}
                    parentContentRef={actionsPropertiesContentRef}
                />
            </div>
            }
        </Root>
    );
}
