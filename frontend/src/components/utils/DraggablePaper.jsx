import React, {useRef} from "react";
import Draggable from "react-draggable";
import {Paper} from "@mui/material";

// Paper of every dialog (set in the theme): the dialog can be moved by its title, so it doesn't cover what is underneath
const DraggablePaper = props => {
    const nodeRef = useRef(null);
    return (
        <Draggable
            nodeRef={nodeRef}
            handle=".MuiDialogTitle-root"
            cancel="button, input"
            bounds="parent"
        >
            <Paper ref={nodeRef} {...props}/>
        </Draggable>
    );
};

export default DraggablePaper;
