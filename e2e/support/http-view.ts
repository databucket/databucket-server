/**
 * An HTML view of one HTTP call (request and response) for the Allure report: an attachment of type text/html, which
 * the report shows in a sandboxed frame (scripts allowed).
 *
 * - a summary line: method, URL, status, time
 * - request, then response: headers (collapsed) and body (open; highlighted JSON)
 * - a Copy button: request and response, with or without headers, as Markdown code blocks that look
 *   right when pasted into Slack (HTML on the clipboard) or Mattermost (Markdown text)
 *
 * The previous view (two attachments, request and response separately) is in git: commit e3141ee.
 */

export interface HttpExchange {
  method: string;
  url: string;
  requestHeaders: Record<string, string>;
  requestBody: unknown;
  status: number;
  statusText: string;
  took: number;
  responseHeaders: Record<string, string>;
  responseBody: unknown;
}

/** Bodies longer than this start collapsed. */
const LONG_BODY_LINES = 300;

const STYLE = `
  body { margin: 0; padding: 10px 12px; font: 13px/1.45 -apple-system, "Segoe UI", Roboto, Arial, sans-serif; color: #1f2328; background: #fff; }
  .mono { font-family: Consolas, "Cascadia Mono", Menlo, monospace; }
  .summary { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
  .url { word-break: break-all; }
  .badge { padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 12px; color: #fff; white-space: nowrap; }
  /* method colors as in Bruno (PATCH, the same as PUT there, gets its own) */
  .GET { background: #059669; } .POST { background: #8e44ad; } .PUT { background: #ca7811; } .DELETE { background: #b91c1c; }
  .PATCH { background: #0e7490; } .HEAD, .OPTIONS { background: #6e7781; }
  .s2 { background: #1a7f37; } .s3 { background: #6e7781; } .s4 { background: #bc4c00; } .s5 { background: #cf222e; }
  .muted { color: #6e7781; }
  .toolbar { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin: 10px 0 4px; }
  .toolbar label { margin-right: 6px; color: #57606a; font-size: 12px; cursor: pointer; user-select: none; }
  button { font: inherit; font-size: 12px; padding: 3px 10px; border: 1px solid #d0d7de; border-radius: 6px; background: #f6f8fa; color: #1f2328; cursor: pointer; }
  button:hover { background: #eaeef2; }
  button.done { border-color: #1a7f37; color: #1a7f37; }
  section { margin-top: 12px; border-top: 1px solid #d8dee4; padding-top: 6px; }
  h3 { margin: 0 0 4px; font-size: 12px; text-transform: uppercase; letter-spacing: .05em; color: #57606a; }
  details { margin: 4px 0; }
  summary { cursor: pointer; color: #57606a; font-size: 12px; user-select: none; }
  table { border-collapse: collapse; font-size: 12px; margin: 4px 0 0 14px; }
  td { padding: 1px 16px 1px 0; vertical-align: top; word-break: break-all; }
  td:first-child { color: #6e7781; white-space: nowrap; }
  pre { margin: 4px 0 0; padding: 8px 10px; background: #f6f8fa; border: 1px solid #d0d7de; border-radius: 6px;
        font-size: 12px; line-height: 1.5; white-space: pre-wrap; word-break: break-word; }
  .error pre { border-color: #ff8182; background: #fff8f8; }
  .k { color: #0550ae; } .str { color: #116329; } .num { color: #953800; } .lit { color: #8250df; font-weight: 600; }
`;

/** Builds the Markdown texts on click; the data of the call is embedded as JSON. */
const SCRIPT = `
  const x = JSON.parse(document.getElementById('exchange').textContent);
  const tick = '\\u0060';
  const fence = tick + tick + tick;
  const esc = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  const body = b => b === undefined || b === null || b === '' ? '' : (typeof b === 'string' ? b : JSON.stringify(b, null, 2));
  const headers = h => Object.entries(h).map(([n, v]) => n + ': ' + v).join('\\n');
  const content = (h, b, withHeaders) => {
    const parts = [];
    if (withHeaders && Object.keys(h).length) parts.push(headers(h));
    if (body(b)) parts.push(body(b));
    return parts.join('\\n\\n') || '(empty)';
  };
  // the copied message:
  //   Request:
  //   POST http://... (inline code)
  //   (block: headers if wanted, body)
  //
  //   Response: 200 OK (21 ms)
  //   (block: headers if wanted, body)
  const line = (code, text) => ({ code, text: text || '' });
  const block = text => ({ block: text });
  const message = h => [
    line(null, 'Request:'),
    line(x.method + ' ' + x.url),
    block(content(x.requestHeaders, x.requestBody, h)),
    line(null, ''),
    line(null, 'Response: ' + x.status + ' ' + x.statusText + ' (' + x.took + ' ms)'),
    block(content(x.responseHeaders, x.responseBody, h)),
  ];
  // Markdown for Mattermost and other editors pasting plain text
  const markdown = parts => parts.map(p => p.block !== undefined
    ? fence + '\\n' + p.block + '\\n' + fence
    : (p.code ? tick + p.code + tick : '') + p.text).join('\\n');
  // HTML for Slack: its editor formats pasted HTML (code, pre), not pasted Markdown
  // lines end with <br> unless a block follows (a block starts on a new line itself); no paragraphs, as Slack puts
  // an empty line after each
  const html = parts => parts.map((p, i) => {
    if (p.block !== undefined) return '<pre>' + esc(p.block) + '</pre>';
    const next = parts[i + 1];
    const content = (p.code ? '<code>' + esc(p.code) + '</code>' : '') + esc(p.text);
    return content + (next && next.block === undefined ? '<br>' : '');
  }).join('');
  // the frame of the report has no clipboard permission: a copy event filled by hand, then the API (text only)
  const copy = parts => {
    const text = markdown(parts);
    let filled = false;
    const onCopy = e => {
      e.clipboardData.setData('text/plain', text);
      e.clipboardData.setData('text/html', html(parts));
      e.preventDefault();
      filled = true;
    };
    document.addEventListener('copy', onCopy);
    try { document.execCommand('copy'); } catch (e) { /* not supported */ }
    document.removeEventListener('copy', onCopy);
    if (filled) return Promise.resolve();
    return navigator.clipboard ? navigator.clipboard.writeText(text) : Promise.reject(new Error('no clipboard'));
  };
  document.querySelectorAll('button.copy').forEach(button => {
    const label = button.textContent;
    button.addEventListener('click', () => {
      const parts = message(document.getElementById('with-headers').checked);
      copy(parts).then(
        () => { button.textContent = 'Copied ✓'; button.classList.add('done'); },
        () => { button.textContent = 'Copy failed'; },
      ).finally(() => setTimeout(() => { button.textContent = label; button.classList.remove('done'); }, 1500));
    });
  });
`;

export function exchangeView(x: HttpExchange): string {
  const failed = x.status >= 400;
  const data = JSON.stringify(x).replace(/</g, '\\u003c');
  return (
    `<!doctype html><html><head><meta charset="utf-8"><style>${STYLE}</style></head><body>` +
    `<div class="summary"><span class="badge ${esc(x.method)}">${esc(x.method)}</span>` +
    `<span class="url mono">${esc(x.url)}</span>` +
    `<span class="badge s${String(x.status)[0]}">${x.status} ${esc(x.statusText)}</span>` +
    `<span class="muted">${x.took} ms</span></div>` +
    `<div class="toolbar"><label><input type="checkbox" id="with-headers"> with headers</label>` +
    `<button class="copy">Copy</button></div>` +
    `<section><h3>Request</h3>${headersBlock(x.requestHeaders)}${bodyBlock(x.requestBody, true)}</section>` +
    `<section class="${failed ? 'error' : ''}"><h3>Response</h3>${headersBlock(x.responseHeaders)}${bodyBlock(x.responseBody, true)}</section>` +
    `<script type="application/json" id="exchange">${data}</script><script>${SCRIPT}</script>` +
    `</body></html>`
  );
}

function headersBlock(headers: Record<string, string>): string {
  const rows = Object.entries(headers);
  if (rows.length === 0) return '<details><summary>Headers (none)</summary></details>';
  const table = rows.map(([name, value]) => `<tr><td>${esc(name)}</td><td>${esc(value)}</td></tr>`).join('');
  return `<details><summary>Headers (${rows.length})</summary><table class="mono">${table}</table></details>`;
}

function bodyBlock(body: unknown, open: boolean): string {
  if (body === undefined || body === null || body === '') return '<details><summary>Body (empty)</summary></details>';
  const text = typeof body === 'string' ? body : JSON.stringify(body, null, 2);
  const lines = text.split('\n').length;
  const long = lines > LONG_BODY_LINES;
  const content = typeof body === 'string' ? esc(body) : highlight(text);
  return `<details${open && !long ? ' open' : ''}><summary>Body${long ? ` (${lines} lines)` : ''}</summary><pre class="mono">${content}</pre></details>`;
}

/** Colors the tokens of pretty-printed JSON: keys, strings, numbers, true / false / null. */
function highlight(json: string): string {
  return esc(json).replace(
    /(&quot;(?:\\.|(?!&quot;).)*?&quot;)(\s*:)?|\b(true|false|null)\b|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)/g,
    (match, str, colon, literal, num) => {
      if (str) return colon ? `<span class="k">${str}</span>${colon}` : `<span class="str">${str}</span>`;
      if (literal) return `<span class="lit">${literal}</span>`;
      return `<span class="num">${num}</span>`;
    },
  );
}

function esc(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
