import React, {useState} from 'react';
import {MTableBodyRow} from '@material-table/core';
import {useTheme} from '@mui/material';

// The position (in the data) of the row being dragged; one drag at a time, so shared by all the rows
let draggedIndex = null;

// A row of a material-table that can be dragged onto another row to move it there (the order of enum items, columns,
// properties, task actions).
// Use: components={{Row: props => <DraggableBodyRow {...props} onMoveRow={(from, to) => setData(movedRow(data, from, to))}/>}}
// Native HTML drag and drop: material-table passes the other props on to the <tr>. The row isn't draggable while a row
// is edited (the inputs need the mouse).
export default function DraggableBodyRow({onMoveRow, ...props}) {
    const theme = useTheme();
    const [dropMarker, setDropMarker] = useState(null); // 'top' | 'bottom' | null
    const index = props.data.tableData.index;
    const draggable = !props.hasAnyEditingRow;

    const line = `inset 0 ${dropMarker === 'top' ? 2 : -2}px 0 ${theme.palette.primary.main}`;

    return (
        <MTableBodyRow
            {...props}
            draggable={draggable}
            sx={{
                cursor: draggable ? 'grab' : undefined,
                boxShadow: dropMarker != null ? line : undefined
            }}
            onDragStart={event => {
                draggedIndex = index;
                event.dataTransfer.effectAllowed = 'move';
                // Firefox starts a drag only with some data set
                event.dataTransfer.setData('text/plain', '');
            }}
            onDragOver={event => {
                if (draggedIndex == null || draggedIndex === index) {
                    return;
                }
                event.preventDefault();
                event.dataTransfer.dropEffect = 'move';
                setDropMarker(draggedIndex > index ? 'top' : 'bottom');
            }}
            onDragLeave={() => setDropMarker(null)}
            onDrop={event => {
                event.preventDefault();
                setDropMarker(null);
                if (draggedIndex != null && draggedIndex !== index) {
                    onMoveRow(draggedIndex, index);
                }
                draggedIndex = null;
            }}
            onDragEnd={() => {
                draggedIndex = null;
                setDropMarker(null);
            }}
        />
    );
}
