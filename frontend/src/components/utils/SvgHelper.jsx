import parse from "html-react-parser";
import DOMPurify from "dompurify";

// The same limit as the server (SvgCheck)
export const SVG_MAX_LENGTH = 32 * 1024;

// An icon is shown to every member of the project: only what draws a picture is kept, no scripts, links,
// styles, foreign content or external resources (the server checks icons too, this also covers older ones)
const PURIFY = {
    USE_PROFILES: {svg: true, svgFilters: true},
    FORBID_TAGS: ['style', 'a', 'image', 'foreignObject', 'script'],
    RETURN_DOM_FRAGMENT: true
};

const EXTERNAL_URL = /url\(\s*['"]?(?!#)/i;

DOMPurify.addHook('afterSanitizeAttributes', node => {
    if (node.attributes == null)
        return;
    for (const attribute of [...node.attributes]) {
        const name = attribute.name.toLowerCase();
        const isLink = name === 'href' || name.endsWith(':href');
        if ((isLink && !attribute.value.trim().startsWith('#')) || EXTERNAL_URL.test(attribute.value))
            node.removeAttribute(attribute.name);
    }
});

/**
 * The sanitized <svg> element of an icon: either a whole <svg> (of any size) or, as icons were stored before,
 * only its content, drawn in a 24x24 box. Null when there is no svg in it.
 */
export const toSvgElement = (markup) => {
    if (markup == null || markup.trim() === '')
        return null;
    const source = markup.trim().toLowerCase().startsWith('<svg') ? markup : `<svg viewBox="0 0 24 24">${markup}</svg>`;
    const svg = [...DOMPurify.sanitize(source, PURIFY).children].find(child => child.localName === 'svg');
    if (svg == null)
        return null;
    // without a viewBox the picture is drawn at its own size: scale it from its width and height
    if (!svg.hasAttribute('viewBox')) {
        const width = parseFloat(svg.getAttribute('width'));
        const height = parseFloat(svg.getAttribute('height'));
        svg.setAttribute('viewBox', width > 0 && height > 0 ? `0 0 ${width} ${height}` : '0 0 24 24');
    }
    return svg;
}

// rendered icons by color and markup (tables render the same icons over and over)
const rendered = new Map();

export const parseCustomSvg = (markup, color) => {
    const key = `${color}\n${markup}`;
    let html = rendered.get(key);
    if (html === undefined) {
        const svg = toSvgElement(markup);
        if (svg != null) {
            svg.setAttribute('width', '24px');
            svg.setAttribute('height', '24px');
            if (color != null) {
                // filled icons take the color as their fill, outlined ones (stroke="currentColor") as the current color
                if (!svg.hasAttribute('fill'))
                    svg.setAttribute('fill', color);
                svg.style.color = color;
            }
            html = svg.outerHTML;
        } else
            html = '';
        if (rendered.size > 500)
            rendered.clear();
        rendered.set(key, html);
    }
    return html !== '' ? parse(html) : null;
};

/**
 * An uploaded SVG file as an icon: the whole <svg> element, sanitized, without the XML declaration, DOCTYPE or
 * comments around it. A single color set on the root (e.g. fill="#5f6368") is removed, so that the icon takes the
 * color of the theme, as the built-in icons do; colors inside the icon stay. Throws the reason it can't be an icon.
 */
export const prepareSvgIcon = (text) => {
    const document = new DOMParser().parseFromString(text, 'image/svg+xml');
    const root = document.documentElement;
    if (document.getElementsByTagName('parsererror').length > 0 || root.localName !== 'svg')
        throw "It isn't an SVG file";

    const isColor = value => value != null && !['none', 'currentcolor', 'inherit'].includes(value.trim().toLowerCase())
        && !value.trim().toLowerCase().startsWith('url(');
    if (isColor(root.getAttribute('fill')))
        root.removeAttribute('fill');
    if (isColor(root.getAttribute('stroke')))
        root.setAttribute('stroke', 'currentColor');

    const svg = toSvgElement(new XMLSerializer().serializeToString(root));
    if (svg == null || svg.children.length === 0)
        throw "There is nothing to draw in it";
    // the size comes from the viewBox; the icon is drawn at the size of the place it is shown in
    svg.removeAttribute('width');
    svg.removeAttribute('height');
    svg.removeAttribute('class');
    svg.removeAttribute('id');

    const structure = svg.outerHTML;
    if (structure.length > SVG_MAX_LENGTH)
        throw `It is larger than ${SVG_MAX_LENGTH / 1024} KB`;
    return structure;
}
