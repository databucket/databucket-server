// A Markdown table of data rows, e.g. to paste into Mattermost or Slack, which show it as a table

const cell = (value) => {
    if (value == null)
        return '';
    const text = typeof value === 'object' ? JSON.stringify(value) : String(value);
    // a | would end the cell, a line break the row
    return text.replace(/\\/g, '\\\\').replace(/\|/g, '\\|').replace(/\r?\n/g, ' ');
}

// columns: [{title, field}], rows: [{field: value}]
export const toMarkdownTable = (columns, rows) => [
    `| ${columns.map(column => cell(column.title)).join(' | ')} |`,
    `| ${columns.map(() => '---').join(' | ')} |`,
    ...rows.map(row => `| ${columns.map(column => cell(row[column.field])).join(' | ')} |`)
].join('\n');

/**
 * Copies the text to the clipboard. navigator.clipboard is available only on HTTPS and localhost, so on a plain HTTP
 * address the text is copied through a hidden text area.
 */
export const copyToClipboard = (text) => {
    if (navigator.clipboard != null && window.isSecureContext)
        return navigator.clipboard.writeText(text);
    return new Promise((resolve, reject) => {
        const area = document.createElement('textarea');
        area.value = text;
        area.setAttribute('readonly', '');
        area.style.position = 'fixed';
        area.style.top = '-1000px';
        document.body.appendChild(area);
        area.select();
        const copied = document.execCommand('copy');
        document.body.removeChild(area);
        copied ? resolve() : reject(new Error('The browser did not allow copying to the clipboard'));
    });
}
