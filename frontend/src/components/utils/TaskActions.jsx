import React, {useContext, useEffect, useState} from "react";
import FormControl from "@material-ui/core/FormControl";
import RadioGroup from "@material-ui/core/RadioGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Radio from "@material-ui/core/Radio";
import FormGroup from "@material-ui/core/FormGroup";
import Checkbox from "@material-ui/core/Checkbox";
import Select from "@material-ui/core/Select";
import MenuItem from "@material-ui/core/MenuItem";
import PropTypes from "prop-types";
import TagsContext from "../../context/tags/TagsContext";
import ActionPropertiesTable from "./ActionPropertiesTable";

TaskActions.propTypes = {
    actions: PropTypes.object,
    properties: PropTypes.array,
    onChange: PropTypes.func.isRequired
}

export default function TaskActions(props) {

    const tagsContext = useContext(TagsContext);
    const {tags, fetchTags} = tagsContext;
    const [actions, setActions] = useState(props.actions);

    useEffect(() => {
        if (tags == null)
            fetchTags();
    }, [tags, fetchTags]);

    const handleActionTypeChange = (event) => {
        setActions({...actions, type: event.target.value})
        props.onChange({...actions, type: event.target.value});
    }

    const handleActionSetTag = (event) => {
        setActions({...actions, setTag: event.target.checked, tagId: tags[0].id});
        props.onChange({...actions, setTag: event.target.checked, tagId: tags[0].id});
    }

    const handleActionSetLock = (event) => {
        setActions({...actions, setLock: event.target.checked});
        props.onChange({...actions, setLock: event.target.checked});
    }

    const handleActionLockChange = (event) => {
        setActions({...actions, lock: (event.target.value === 'true')});
        props.onChange({...actions, lock: (event.target.value === 'true')});
    }

    const handleTagChange = (event) => {
        setActions({...actions, tagId: event.target.value});
        props.onChange({...actions, tagId: event.target.value});
    }

    const onActionPropertiesChange = (updatedProperties) => {
        setActions({...actions, properties: updatedProperties});
        props.onChange({...actions, properties: updatedProperties});
    }

    return (
        <div>
            <div style={{marginLeft: '30px', marginTop: '20px'}}>
                <FormControl component="fieldset">
                    <RadioGroup row value={actions.type} onChange={handleActionTypeChange}>
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
                    </RadioGroup>
                    {actions.type === 'modify' &&
                    <div>
                        {tags != null && tags.length > 0 &&
                        <FormGroup row>
                            <FormControlLabel
                                label="Set tag"
                                control={<Checkbox checked={actions.setTag} onChange={handleActionSetTag}/>}
                            />
                            {actions.setTag === true &&
                            <FormControlLabel
                                label=''
                                labelPlacement="start"
                                control={
                                    <Select
                                        id="tag-select"
                                        onChange={handleTagChange}
                                        value={actions.tagId}
                                    >
                                        {tags.map(tag => (
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
                                control={<Checkbox checked={actions.setLock} onChange={handleActionSetLock}/>}
                            />
                            {actions.setLock === true &&
                            <RadioGroup row value={actions.lock} onChange={handleActionLockChange}>
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
            {actions.type === 'modify' &&
            <ActionPropertiesTable
                data={actions.properties}
                properties={props.properties}
                onChange={onActionPropertiesChange}
            />
            }
        </div>
    );
};
