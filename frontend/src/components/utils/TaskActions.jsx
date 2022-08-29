import React from "react";
import FormControl from "@material-ui/core/FormControl";
import RadioGroup from "@material-ui/core/RadioGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Radio from "@material-ui/core/Radio";
import FormGroup from "@material-ui/core/FormGroup";
import Checkbox from "@material-ui/core/Checkbox";
import Select from "@material-ui/core/Select";
import MenuItem from "@material-ui/core/MenuItem";
import PropTypes from "prop-types";
import ActionPropertiesTable from "./ActionPropertiesTable";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(() => ({
    settingsContainer: {
        height: '27%',
        marginLeft: '20px',
        marginTop: '10px'
    },
    propertiesContainer: {
        height: '72%'
    }
}));

TaskActions.propTypes = {
    actions: PropTypes.object,
    properties: PropTypes.array,
    tags: PropTypes.array,
    onChange: PropTypes.func.isRequired,
    pageSize: PropTypes.number
}

export default function TaskActions(props) {

    const classes = useStyles();
    const actionsPropertiesContentRef = React.useRef(null);

    const handleActionTypeChange = (event) => {
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
        <div style={{height: '95%'}}>
            <div className={classes.settingsContainer}>
                <FormControl component="fieldset">
                    <RadioGroup row value={props.actions.type || ''} onChange={handleActionTypeChange}>
                        <FormControlLabel
                            value="remove"
                            control={<Radio/>}
                            label="Remove data"
                        />
                        <FormControlLabel
                            value="modify"
                            control={<Radio/>}
                            label="Modify data"
                        />
                        <FormControlLabel
                            value="clear history"
                            control={<Radio/>}
                            label="Clear history"
                        />
                    </RadioGroup>
                    {props.actions.type === 'modify' &&
                    <div>
                        {props.tags != null && props.tags.length > 0 &&
                        <FormGroup row>
                            <FormControlLabel
                                label="Set tag"
                                control={<Checkbox checked={props.actions.setTag || false} onChange={handleActionSetTag}/>}
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
                                control={<Checkbox checked={props.actions.setReserved || false} onChange={handleActionSetReserved}/>}
                            />
                            {props.actions.setReserved === true &&
                            <RadioGroup row value={props.actions.reserved || false} onChange={handleActionReserveChange}>
                                <FormControlLabel
                                    value={true}
                                    control={<Radio/>}
                                    label="True"
                                />
                                <FormControlLabel
                                    value={false}
                                    control={<Radio/>}
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
        </div>
    );
};
