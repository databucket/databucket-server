// Generators of the "Generate data" dialog: one row per property, turned into the spec of
// POST /api/bucket/{bucket}/generate (see DataGenerator on the server).

export const KIND_TEXT = 'text';         // a Datafaker expression #{...}, a text with {n}, or a constant text
export const KIND_NUMBER = 'number';     // {number: [min, max], decimals}
export const KIND_ONE_OF = 'oneOf';      // {oneOf: [values]}
export const KIND_DATE = 'date';         // {date: [from, to]}
export const KIND_DATETIME = 'datetime'; // {datetime: [from, to]}

export const KINDS = [
    {value: KIND_TEXT, label: 'Text / Datafaker'},
    {value: KIND_NUMBER, label: 'Number'},
    {value: KIND_ONE_OF, label: 'One of'},
    {value: KIND_DATE, label: 'Date'},
    {value: KIND_DATETIME, label: 'Date and time'},
];

export const LOCALES = ['en', 'pl', 'de', 'fr', 'es', 'it', 'nl', 'pt', 'cs', 'sv'];

// Datafaker expressions guessed from the name of a text property (the first match wins)
const TEXT_GUESSES = [
    [/e-?mail/, 'user-{n}@example.com'],
    [/first.?name|given.?name|forename/, '#{Name.firstName}'],
    [/last.?name|surname|family.?name/, '#{Name.lastName}'],
    [/user.?name|login|nick/, 'user-{n}'],
    [/full.?name|^name$|person/, '#{Name.fullName}'],
    [/password|secret/, "#{regexify '[A-Za-z0-9]{12}'}"],
    [/phone|mobile|tel/, "#{numerify '###-###-###'}"],
    [/street/, '#{Address.streetAddress}'],
    [/city|town/, '#{Address.city}'],
    [/zip|post.?code|postal/, '#{Address.zipCode}'],
    [/country/, '#{Address.country}'],
    [/address/, '#{Address.fullAddress}'],
    [/company|organi[sz]ation|employer/, '#{Company.name}'],
    [/iban|account.?number/, '#{Finance.iban}'],
    [/url|website|link/, '#{Internet.url}'],
    [/description|comment|note|text/, '#{Lorem.sentence}'],
    [/^id$|number|code|ref/, 'ID-{n}'],
];

const pad = n => String(n).padStart(2, '0');
const isoDate = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const yearsAgo = years => {
    const d = new Date();
    d.setFullYear(d.getFullYear() - years);
    return d;
}

// the last node of the path, lower case: "$.customer.firstName" -> "firstname"
const nameOf = path => path.substring(path.lastIndexOf('.') + 1).toLowerCase();

// A generator row for a column of the view: {path, title, type, kind, ...}
export const guessRow = column => {
    const path = column.source;
    const name = nameOf(path);
    const base = {path, title: column.title, type: column.type};
    if (column.lookup != null)
        return {...base, kind: KIND_ONE_OF, values: Object.keys(column.lookup).join(', ')};
    switch (column.type) {
        case 'boolean':
            return {...base, kind: KIND_ONE_OF, values: 'true, false'};
        case 'numeric':
            if (/age/.test(name))
                return {...base, kind: KIND_NUMBER, min: '18', max: '90', decimals: '0'};
            if (/price|amount|balance|cost|salary|total|value/.test(name))
                return {...base, kind: KIND_NUMBER, min: '0', max: '1000', decimals: '2'};
            return {...base, kind: KIND_NUMBER, min: '0', max: '1000', decimals: '0'};
        case 'date':
            if (/birth/.test(name))
                return {...base, kind: KIND_DATE, from: '1950-01-01', to: '2005-12-31'};
            return {...base, kind: KIND_DATE, from: isoDate(yearsAgo(1)), to: isoDate(new Date())};
        case 'datetime':
            return {...base, kind: KIND_DATETIME, from: isoDate(yearsAgo(1)), to: isoDate(new Date())};
        case 'time':
            return {...base, kind: KIND_TEXT, text: "#{regexify '(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]'}"};
        default: {
            const guess = TEXT_GUESSES.find(([pattern]) => pattern.test(name));
            return {...base, kind: KIND_TEXT, text: guess != null ? guess[1] : '#{Lorem.word}'};
        }
    }
}

// The rows for the property columns of the view (each property once)
export const rowsFromColumns = columns => {
    const seen = new Set();
    return columns
        .filter(c => c.source != null && c.source.startsWith('$.') && !seen.has(c.source) && seen.add(c.source))
        .map(guessRow);
}

// The row with another kind of generator; values of that kind not set yet get a default
export const withKind = (row, kind) => {
    switch (kind) {
        case KIND_NUMBER:
            return {min: '0', max: '100', decimals: '0', ...row, kind};
        case KIND_ONE_OF:
            return {values: '', ...row, kind};
        case KIND_DATE:
        case KIND_DATETIME:
            return {from: isoDate(yearsAgo(1)), to: isoDate(new Date()), ...row, kind};
        default:
            return {text: '', ...row, kind};
    }
}

export const emptyRow = () => ({path: '$.', title: null, type: 'string', kind: KIND_TEXT, text: ''});

// a value of "One of": true/false, a number for a numeric property, otherwise the text
const oneOfValue = (text, type) => {
    if (text === 'true' || text === 'false')
        return text === 'true';
    if (type === 'numeric' && text !== '' && !Number.isNaN(Number(text)))
        return Number(text);
    return text;
}

const number = (text, label, path) => {
    const value = Number(String(text).trim().replace(',', '.'));
    if (String(text).trim() === '' || Number.isNaN(value))
        throw `${path}: ${label} is not a number`;
    return value;
}

// The generator of one row; throws a message when the row is incomplete
const generatorOf = row => {
    switch (row.kind) {
        case KIND_NUMBER:
            return {
                number: [number(row.min, 'min', row.path), number(row.max, 'max', row.path)],
                decimals: number(row.decimals || '0', 'decimals', row.path)
            };
        case KIND_ONE_OF: {
            const values = String(row.values ?? '').split(',').map(v => v.trim()).filter(v => v !== '');
            if (values.length === 0)
                throw `${row.path}: give the values, separated with commas`;
            return {oneOf: values.map(v => oneOfValue(v, row.type))};
        }
        case KIND_DATE:
        case KIND_DATETIME:
            if (!row.from || !row.to)
                throw `${row.path}: give both dates`;
            return {[row.kind]: [row.from, row.to]};
        default:
            if (row.text == null || row.text === '')
                throw `${row.path}: give a text or an expression`;
            return row.text;
    }
}

// The fields of the request: path -> generator; throws a message for the first incomplete row
export const toSpec = rows => {
    if (rows.length === 0)
        throw 'Add at least one property';
    const spec = {};
    rows.forEach(row => {
        if (!/^\$\.[^.\s]+(\.[^.\s]+)*$/.test(row.path ?? ''))
            throw `"${row.path}" is not a property path such as $.name or $.address.city`;
        if (spec[row.path] !== undefined)
            throw `${row.path} is given more than once`;
        spec[row.path] = generatorOf(row);
    });
    return spec;
}
