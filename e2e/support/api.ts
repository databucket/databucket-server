import type { APIRequestContext } from '@playwright/test';
import * as allure from 'allure-js-commons';
import { exchangeView } from './http-view';

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
 * with the request and the response attached as one HTML view (see http-view.ts).
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
        const view = exchangeView({
          method,
          url: response.url(),
          requestHeaders: maskHeaders(headers),
          requestBody: mask(options.data),
          status: result.status,
          statusText: response.statusText(),
          took,
          responseHeaders: shownHeaders(result.headers),
          responseBody: mask(result.body),
        });
        await allure.attachment('request / response', view, 'text/html');
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

function maskHeaders(headers: Record<string, string>): Record<string, string> {
  return Object.fromEntries(
    Object.entries(headers).map(([name, value]) => [name, SECRET_HEADERS.has(name.toLowerCase()) ? value.replace(/^(Bearer )?.+$/, '$1***') : value]),
  );
}

/** The response headers worth seeing (the others are the same for every call). */
function shownHeaders(headers: Record<string, string>): Record<string, string> {
  return Object.fromEntries(Object.entries(headers).filter(([name]) => ['content-type', 'location'].includes(name)));
}
