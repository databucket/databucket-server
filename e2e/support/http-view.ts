/**
 * An HTML view of one HTTP call (request and response) for the Allure report: an attachment of type text/html, which
 * the report shows in a sandboxed frame (scripts allowed).
 *
 * - a summary line: method, URL, status, time
 * - request, then response: headers (collapsed) and body (open; highlighted JSON)
 * - buttons copying the request, the response or both, with or without headers, as Markdown code blocks that look
 *   right when pasted into Slack or Mattermost
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
  .GET { background: #1f6feb; } .POST { background: #1a7f37; } .PUT { background: #9a6700; } .DELETE { background: #cf222e; }
  .PATCH { background: #8250df; }
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
  const fence = '\\u0060\\u0060\\u0060';
  const body = b => b === undefined || b === null || b === '' ? '' : (typeof b === 'string' ? b : JSON.stringify(b, null, 2));
  const headers = h => Object.entries(h).map(([n, v]) => n + ': ' + v).join('\\n');
  const block = (h, b, withHeaders) => {
    const parts = [];
    if (withHeaders && Object.keys(h).length) parts.push(headers(h));
    if (body(b)) parts.push(body(b));
    return fence + '\\n' + (parts.join('\\n\\n') || '(empty)') + '\\n' + fence;
  };
  const call = '\\u0060' + x.method + ' ' + x.url + '\\u0060';
  const result = x.status + ' ' + x.statusText + ' (' + x.took + ' ms)';
  const texts = {
    request: h => call + '\\n' + block(x.requestHeaders, x.requestBody, h),
    response: h => call + ' → ' + result + '\\n' + block(x.responseHeaders, x.responseBody, h),
    all: h => call + ' → ' + result + '\\nRequest:\\n' + block(x.requestHeaders, x.requestBody, h) +
              '\\nResponse:\\n' + block(x.responseHeaders, x.responseBody, h),
  };
  // the frame of the report has no clipboard permission: the old way (a selected text area) first, then the API
  const copy = text => {
    const area = document.createElement('textarea');
    area.value = text;
    area.style.position = 'fixed';
    area.style.opacity = '0';
    document.body.appendChild(area);
    area.select();
    let ok = false;
    try { ok = document.execCommand('copy'); } catch (e) { ok = false; }
    area.remove();
    if (ok) return Promise.resolve();
    return navigator.clipboard ? navigator.clipboard.writeText(text) : Promise.reject(new Error('no clipboard'));
  };
  document.querySelectorAll('button[data-copy]').forEach(button => {
    const label = button.textContent;
    button.addEventListener('click', () => {
      const text = texts[button.dataset.copy](document.getElementById('with-headers').checked);
      copy(text).then(
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
    `<button data-copy="request">Copy request</button><button data-copy="response">Copy response</button>` +
    `<button data-copy="all">Copy both</button></div>` +
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
