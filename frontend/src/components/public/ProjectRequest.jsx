import React from "react";
import {Box, Checkbox, FormControl, FormControlLabel, FormHelperText, Input, InputLabel, Typography} from "@mui/material";

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
            {/* as the other fields of the sign-in pages (e.g. change password): a label and an underline */}
            <FormControl fullWidth sx={{mt: 3}}>
                <InputLabel htmlFor="project-request-message">Message (optional)</InputLabel>
                <Input
                    id="project-request-message"
                    multiline
                    minRows={1}
                    maxRows={8}
                    value={message}
                    onChange={e => onMessageChange(e.target.value.slice(0, MESSAGE_MAX))}
                />
                <FormHelperText>{`${message.length}/${MESSAGE_MAX}`}</FormHelperText>
            </FormControl>
        </Box>
    );
}
