import React from "react";
import { styled } from '@mui/material/styles';
import FormControl from "@mui/material/FormControl";
import RadioGroup from "@mui/material/RadioGroup";
import FormControlLabel from "@mui/material/FormControlLabel";
import Radio from "@mui/material/Radio";
import FormGroup from "@mui/material/FormGroup";
import Checkbox from "@mui/material/Checkbox";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import PropTypes from "prop-types";
import ActionPropertiesTable from "./ActionPropertiesTable";
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
    pageSize: PropTypes.number
}

export default function TaskActions(props) {


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
        <Root style={{height: '95%'}}>
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
        </Root>
    );
}
