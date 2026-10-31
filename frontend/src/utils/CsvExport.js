// CSV (RFC 4180) with a UTF-8 BOM, so that Excel reads national characters correctly
const csvValue = (value) => {
    if (value == null)
        return '';
    const text = typeof value === 'object' ? JSON.stringify(value) : String(value);
    return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

// columns: [{title, field}], rows: [{field: value}]
export const exportCsv = (fileName, columns, rows) => {
    const lines = [
        columns.map(column => csvValue(column.title)).join(','),
        ...rows.map(row => columns.map(column => csvValue(row[column.field])).join(','))
    ];
    const blob = new Blob(['﻿' + lines.join('\r\n')], {type: 'text/csv;charset=utf-8'});
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `${fileName}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
}
