import React from "react";
import {Box, Checkbox, FormControl, FormControlLabel, FormHelperText, OutlinedInput, Typography} from "@mui/material";

export const MESSAGE_MAX = 500;

/**
 * While registering: the public projects to ask for, and a message for the administrators.
 * Shown only when there are public projects (the server lists them only when mail is configured).
 */
export default function ProjectRequest({projects, selected, onChange, message, onMessageChange}) {
    if (projects == null || projects.length === 0)
        return null;

    const toggle = id => onChange(selected.includes(id) ? selected.filter(item => item !== id) : [...selected, id]);

    return (
        <Box sx={{width: '85%', mt: 3, mb: 1}}>
            <Typography variant="body2" sx={{mb: 0.5}}>
                Ask for access to projects:
            </Typography>
            <Box sx={{maxHeight: 180, overflowY: 'auto', pl: 0.5}}>
                {projects.map(project => (
                    <FormControlLabel
                        key={project.id}
                        sx={{display: 'flex', alignItems: 'flex-start', mr: 0}}
                        control={<Checkbox size="small" sx={{pt: 0.25, pb: 0.25}} checked={selected.includes(project.id)}
                                           onChange={() => toggle(project.id)}/>}
                        label={
                            <Box>
                                <Typography variant="body2">{project.name}</Typography>
                                {project.description &&
                                    <Typography variant="caption" color="text.secondary">{project.description}</Typography>}
                            </Box>
                        }
                    />
                ))}
            </Box>
            {/* a box with its name above it, as in the confirmations (e.g. reset password) */}
            <Typography variant="body2" sx={{mt: 3, mb: 0.5}}>
                Message (optional):
            </Typography>
            <FormControl fullWidth>
                <OutlinedInput
                    id="project-request-message"
                    multiline
                    minRows={3}
                    maxRows={8}
                    inputProps={{'aria-label': 'message'}}
                    value={message}
                    onChange={e => onMessageChange(e.target.value.slice(0, MESSAGE_MAX))}
                />
                <FormHelperText>{`${message.length}/${MESSAGE_MAX}`}</FormHelperText>
            </FormControl>
        </Box>
    );
}
