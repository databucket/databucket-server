import React from "react";
import {getJsonPalette} from "./JsonColors";

// a string (a key when a colon follows), a number (not digits within a word, e.g. a part of an id like 43e2),
// or true/false/null
const TOKEN = /("(?:\\.|[^"\\])*")(\s*:)?|(?<![\w.])(-?\d+(?:\.\d+)?)(?![\w.])|\b(true|false|null)\b/g;

// Colors a piece of pretty-printed JSON: a line, or a part of a line (the diff viewer passes the changed words
// separately). A string cut in two by such a part is left plain. No text: the empty side of a line in the split view.
export const highlightJson = (text, dark) => {
    if (text == null)
        return text;
    // the palette chosen for the theme in the data details, as in the JSON editor there
    const colors = getJsonPalette(dark ? 'dark' : 'light');
    const parts = [];
    let last = 0;
    let match;
    TOKEN.lastIndex = 0;
    while ((match = TOKEN.exec(text)) !== null) {
        if (match.index > last)
            parts.push(text.substring(last, match.index));
        const [, string, colon, number, literal] = match;
        if (string != null) {
            parts.push(<span key={match.index} style={{color: colon != null ? colors.key : colors.string}}>{string}</span>);
            if (colon != null)
                parts.push(colon);
        } else if (number != null)
            parts.push(<span key={match.index} style={{color: colors.number}}>{number}</span>);
        else
            parts.push(<span key={match.index} style={{color: colors.literal}}>{literal}</span>);
        last = TOKEN.lastIndex;
    }
    if (last < text.length)
        parts.push(text.substring(last));
    return <>{parts}</>;
}
