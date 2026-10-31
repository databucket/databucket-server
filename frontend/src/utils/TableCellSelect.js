// A double click selects the whole text of a table cell instead of one word, ready to copy.
// Not in cells with buttons (actions) or fields (a row being edited). Use as onDoubleClick of an element around the table.
export const selectCellText = (event) => {
    const cell = event.target.closest('tbody td');
    if (cell == null || cell.querySelector('button, input, textarea') != null)
        return;
    const range = document.createRange();
    range.selectNodeContents(cell);
    const selection = window.getSelection();
    selection.removeAllRanges();
    selection.addRange(range);
}
