// CSV import, the reverse of CsvExport: the header holds the titles of the view's columns, the values are as the table
// shows them (tag and enum names instead of ids).

// RFC 4180: quoted values may hold the delimiter, quotes ("") and line breaks; a UTF-8 BOM is skipped. The delimiter is
// a comma, or a semicolon when the header has semicolons and no commas (Excel with a regional list separator).
export const parseCsv = (text) => {
    const input = text.charCodeAt(0) === 0xFEFF ? text.substring(1) : text;
    const firstLine = input.split(/\r?\n/, 1)[0];
    const delimiter = firstLine.includes(';') && !firstLine.includes(',') ? ';' : ',';

    const records = [];
    let record = [];
    let value = '';
    let quoted = false;
    for (let i = 0; i < input.length; i++) {
        const c = input[i];
        if (quoted) {
            if (c === '"') {
                if (input[i + 1] === '"') {
                    value += '"';
                    i++;
                } else
                    quoted = false;
            } else
                value += c;
        } else if (c === '"')
            quoted = true;
        else if (c === delimiter) {
            record.push(value);
            value = '';
        } else if (c === '\r' || c === '\n') {
            if (c === '\r' && input[i + 1] === '\n')
                i++;
            record.push(value);
            records.push(record);
            record = [];
            value = '';
        } else
            value += c;
    }
    if (value !== '' || record.length > 0) {
        record.push(value);
        records.push(record);
    }
    // empty lines, e.g. the one after the last line break
    return records.filter(r => r.some(v => v !== ''));
}

// columns the database sets itself; whether the rows are reserved is chosen in the import dialog
const SYSTEM_SOURCES = ['id', 'reserved', 'owner', 'createdAt', 'createdBy', 'modifiedAt', 'modifiedBy'];

const BOOLEANS = {'true': true, 'false': false, '1': true, '0': false, 'yes': true, 'no': false};

// a value shown by a lookup (tag, enum) back to its key; the key itself is accepted too
const fromLookup = (lookup, text) => {
    const key = Object.keys(lookup).find(k => lookup[k] === text) ?? Object.keys(lookup).find(k => k === text);
    return key;
}

// "$.address.city" -> properties.address.city
const setProperty = (properties, source, value) => {
    const nodes = source.substring(2).split('.');
    let obj = properties;
    nodes.slice(0, -1).forEach(node => {
        if (obj[node] == null || typeof obj[node] !== 'object')
            obj[node] = {};
        obj = obj[node];
    });
    obj[nodes[nodes.length - 1]] = value;
}

// throws a message when the text doesn't fit the column
const convertValue = (column, text) => {
    if (column.lookup != null) {
        const key = fromLookup(column.lookup, text);
        if (key === undefined)
            throw `unknown value "${text}"`;
        return column.type === 'numeric' ? Number(key) : key;
    }
    switch (column.type) {
        case 'numeric': {
            const number = Number(text.trim().replace(',', '.'));
            if (text.trim() === '' || Number.isNaN(number))
                throw `"${text}" is not a number`;
            return number;
        }
        case 'boolean': {
            const bool = BOOLEANS[text.trim().toLowerCase()];
            if (bool === undefined)
                throw `"${text}" is not true or false`;
            return bool;
        }
        case 'date':
        case 'datetime':
        case 'time':
            return text;
        default:
            // objects and arrays are exported as JSON
            if (/^\s*[[{]/.test(text)) {
                try {
                    return JSON.parse(text);
                } catch (e) {
                    return text;
                }
            }
            return text;
    }
}

// columns: the table columns ({title, field, source, type, lookup}); records: parsed CSV, the first one the header.
// Returns the rows to create ({tagId, properties}), the imported and ignored column titles, and the errors.
export const toImportRows = (columns, records) => {
    const errors = [];
    if (records.length === 0)
        return {rows: [], imported: [], ignored: [], errors: ['The file is empty.']};

    const header = records[0].map(title => title.trim());
    const mapping = header.map(title => {
        const column = columns.find(c => c.title === title);
        return column != null && !SYSTEM_SOURCES.includes(column.source) ? column : null;
    });
    const imported = header.filter((title, i) => mapping[i] != null);
    const ignored = header.filter((title, i) => mapping[i] == null && title !== '');

    header.forEach((title, i) => {
        if (mapping[i] != null && header.indexOf(title) !== i)
            errors.push(`The column "${title}" is given more than once.`);
    });
    if (imported.length === 0)
        errors.push('No column of the file matches a column of the view.');

    const rows = records.slice(1).map((record, r) => {
        const row = {properties: {}};
        mapping.forEach((column, i) => {
            const text = record[i];
            if (column == null || text == null || text === '')
                return;
            try {
                const value = convertValue(column, text);
                if (column.source === 'tagId')
                    row.tagId = value;
                else if (column.source.startsWith('$.'))
                    setProperty(row.properties, column.source, value);
            } catch (message) {
                // rows counted without the header (a quoted value may span lines, so not the line of the file)
                errors.push(`Row ${r + 1}, column "${column.title}": ${message}.`);
            }
        });
        return row;
    });

    return {rows, imported, ignored, errors};
}
