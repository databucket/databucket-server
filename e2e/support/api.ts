import type { APIRequestContext } from '@playwright/test';
import * as allure from 'allure-js-commons';

/** Header selecting version 2 of the data API. */
export const API_V2 = { 'Databucket-Api-Version': '2' };

export interface ApiResponse<T = any> {
  status: number;
  headers: Record<string, string>;
  body: T;
  /** The response body as received. */
  text: string;
}

export interface CallOptions {
  data?: unknown;
  headers?: Record<string, string>;
  /** Fail the call when the status differs. */
  expectStatus?: number;
}

const SECRET_FIELDS = new Set(['password', 'newPassword', 'token', 'ssoToken']);
const SECRET_HEADERS = new Set(['authorization', 'cookie', 'set-cookie']);

/**
 * A REST client for the tests. Every call is a step in the report ("demo-admin: POST /api/buckets → 201")
 * with the request and the response attached as readable text: method, URL, headers and pretty-printed JSON.
 * Tokens and passwords are masked, so reports can be shared.
 *
 * `quiet` clients (used to prepare test data) skip the steps and attachments.
 */
export class Api {
  constructor(
    private readonly request: APIRequestContext,
    readonly user: string,
    readonly token?: string,
    private readonly quiet = false,
  ) {}

  /** The same user with his token. */
  withToken(token: string): Api {
    return new Api(this.request, this.user, token, this.quiet);
  }

  /** The same user, reported in the steps. */
  loud(): Api {
    return new Api(this.request, this.user, this.token, false);
  }

  get = <T = any>(path: string, options?: CallOptions) => this.call<T>('GET', path, options);
  post = <T = any>(path: string, options?: CallOptions) => this.call<T>('POST', path, options);
  put = <T = any>(path: string, options?: CallOptions) => this.call<T>('PUT', path, options);
  delete = <T = any>(path: string, options?: CallOptions) => this.call<T>('DELETE', path, options);

  async call<T = any>(method: string, path: string, options: CallOptions = {}): Promise<ApiResponse<T>> {
    const headers: Record<string, string> = { Accept: 'application/json', ...options.headers };
    if (this.token) headers.Authorization = `Bearer ${this.token}`;
    if (options.data !== undefined) headers['Content-Type'] = 'application/json';

    const send = async (step?: allure.StepContext) => {
      const start = Date.now();
      const response = await this.request.fetch(path, {
        method,
        headers,
        data: options.data === undefined ? undefined : JSON.stringify(options.data),
        failOnStatusCode: false,
      });
      const took = Date.now() - start;
      const text = await response.text();
      const result: ApiResponse<T> = { status: response.status(), headers: response.headers(), body: parse(text), text };

      if (step) {
        await step.displayName(`${this.user}: ${method} ${path} → ${result.status}`);
        await step.parameter('time', `${took} ms`);
        await allure.attachment('request', formatRequest(method, response.url(), headers, options.data), 'text/plain');
        await allure.attachment(`response ${result.status}`, formatResponse(result, response.statusText()), 'text/plain');
      }
      if (options.expectStatus !== undefined && result.status !== options.expectStatus)
        throw new Error(`${method} ${path}: expected status ${options.expectStatus}, got ${result.status}\n${text.slice(0, 2000)}`);
      return result;
    };

    if (this.quiet) return send();
    return allure.step(`${this.user}: ${method} ${path}`, step => send(step));
  }
}

function parse(text: string): any {
  if (!text) return undefined;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function mask(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(mask);
  if (value && typeof value === 'object')
    return Object.fromEntries(Object.entries(value).map(([k, v]) => [k, SECRET_FIELDS.has(k) ? '***' : mask(v)]));
  return value;
}

function formatHeaders(headers: Record<string, string>): string {
  return Object.entries(headers)
    .map(([name, value]) => `${name}: ${SECRET_HEADERS.has(name.toLowerCase()) ? value.replace(/(Bearer )?.+/, '$1***') : value}`)
    .join('\n');
}

function formatBody(body: unknown): string {
  if (body === undefined || body === '') return '';
  return typeof body === 'string' ? body : JSON.stringify(mask(body), null, 2);
}

function formatRequest(method: string, url: string, headers: Record<string, string>, data: unknown): string {
  return [`${method} ${url}`, formatHeaders(headers), '', formatBody(data)].join('\n').trimEnd();
}

function formatResponse(response: ApiResponse, statusText: string): string {
  const shown = Object.fromEntries(
    Object.entries(response.headers).filter(([name]) => ['content-type', 'location'].includes(name)),
  );
  return [`HTTP ${response.status} ${statusText}`, formatHeaders(shown), '', formatBody(response.body)].join('\n').trimEnd();
}
