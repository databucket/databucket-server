/**
 * HTML views of an HTTP request and response for the Allure report (attachments of type text/html, shown by the
 * report in a frame): the method / status line, a table of headers and the body as highlighted JSON.
 */

const STYLE = `
  body { margin: 0; padding: 12px; font: 13px/1.45 -apple-system, "Segoe UI", Roboto, Arial, sans-serif; color: #1f2328; background: #fff; }
  .line { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; flex-wrap: wrap; }
  .badge { padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 12px; color: #fff; }
  .GET { background: #1f6feb; } .POST { background: #1a7f37; } .PUT { background: #9a6700; } .DELETE { background: #cf222e; }
  .PATCH { background: #8250df; }
  .s2 { background: #1a7f37; } .s3 { background: #6e7781; } .s4 { background: #bc4c00; } .s5 { background: #cf222e; }
  .url { font-family: Consolas, "Cascadia Mono", Menlo, monospace; word-break: break-all; }
  .muted { color: #6e7781; }
  h4 { margin: 14px 0 6px; font-size: 11px; text-transform: uppercase; letter-spacing: .05em; color: #6e7781; }
  table { border-collapse: collapse; font-family: Consolas, "Cascadia Mono", Menlo, monospace; font-size: 12px; }
  td { padding: 2px 16px 2px 0; vertical-align: top; }
  td:first-child { color: #6e7781; white-space: nowrap; }
  pre { margin: 0; padding: 10px 12px; background: #f6f8fa; border: 1px solid #d0d7de; border-radius: 6px;
        font: 12px/1.5 Consolas, "Cascadia Mono", Menlo, monospace; white-space: pre-wrap; word-break: break-word; }
  .k { color: #0550ae; } .str { color: #116329; } .num { color: #953800; } .lit { color: #8250df; font-weight: 600; }
`;

export function requestView(method: string, url: string, headers: Record<string, string>, body: unknown): string {
  return page(
    `<div class="line"><span class="badge ${esc(method)}">${esc(method)}</span><span class="url">${esc(url)}</span></div>` +
      headersTable(headers) +
      bodyBlock(body),
  );
}

export function responseView(status: number, statusText: string, took: number, headers: Record<string, string>, body: unknown): string {
  return page(
    `<div class="line"><span class="badge s${String(status)[0]}">${status} ${esc(statusText)}</span>` +
      `<span class="muted">${took} ms</span></div>` +
      headersTable(headers) +
      bodyBlock(body),
  );
}

function page(content: string): string {
  return `<!doctype html><html><head><meta charset="utf-8"><style>${STYLE}</style></head><body>${content}</body></html>`;
}

function headersTable(headers: Record<string, string>): string {
  const rows = Object.entries(headers);
  if (rows.length === 0) return '';
  return `<h4>Headers</h4><table>${rows.map(([name, value]) => `<tr><td>${esc(name)}</td><td>${esc(value)}</td></tr>`).join('')}</table>`;
}

function bodyBlock(body: unknown): string {
  if (body === undefined || body === '') return '<h4>Body</h4><span class="muted">(empty)</span>';
  const content = typeof body === 'string' ? esc(body) : highlight(JSON.stringify(body, null, 2));
  return `<h4>Body</h4><pre>${content}</pre>`;
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
