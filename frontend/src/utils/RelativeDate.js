// A date relative to the moment the rules are read, as the server reads it (LogicSql.now): @now, @now-2h, @now+1d
// (units s, m, h, d, w). The query builder keeps it in the filter as this text instead of a fixed date.

export const RELATIVE_DATE = /^@now([+-]\d{1,6}[smhdw])?$/;

export const isRelative = value => typeof value === 'string' && value.trim().startsWith('@now');

export const isValidRelative = value => typeof value === 'string' && RELATIVE_DATE.test(value.trim());

/**
 * The date or datetime widget config of the query builder with relative dates allowed: exported to the filter as the
 * text, imported back as it is, shown as it is. Fixed dates go through the original widget.
 */
export const withRelativeDateFormats = base => ({
    ...base,
    formatValue: function (val, fieldDef, wgtDef, isForDisplay, ...rest) {
        return isRelative(val)
            ? (isForDisplay ? val : JSON.stringify(val))
            : base.formatValue.call(this, val, fieldDef, wgtDef, isForDisplay, ...rest);
    },
    sqlFormatValue: function (val, ...rest) {
        return isRelative(val) ? `'${val.trim()}'` : base.sqlFormatValue.call(this, val, ...rest);
    },
    // to the filter: a relative date as its text (the server reads it), a fixed date as a Date (ISO text in the JSON)
    jsonLogic: function (val, ...rest) {
        return isRelative(val) ? val.trim() : base.jsonLogic.call(this, val, ...rest);
    },
    // from the filter: a relative date as it is; a fixed date (ISO text or ms) as a Date, which the builder then
    // formats, as its own import does
    jsonLogicImport: function (val) {
        if (isRelative(val))
            return val.trim();
        if (val instanceof Date)
            return val;
        const epoch = typeof val === 'number' || (typeof val === 'string' && val !== '' && !isNaN(val));
        const date = new Date(epoch ? Number(val) : val);
        if (isNaN(date))
            throw new Error('Invalid date');
        return date;
    }
});
