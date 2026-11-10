/**
 * What the report shows of the API calls (see api.ts and http-view.ts). Names are compared in lower case.
 */
export const reportConfig = {
  /**
   * Response headers shown in the report (and copied "with headers"); the others (Cache-Control, Vary,
   * X-Frame-Options...) are the same for every call. Add e.g. 'x-correlation-id' or 'traceparent' when needed.
   */
  responseHeaders: ['content-type', 'location', 'date', 'x-request-id'],

  /** Request and response headers whose values are masked (***), so reports can be shared. */
  secretHeaders: ['authorization', 'cookie', 'set-cookie'],

  /** JSON fields, at any depth of a request or response body, whose values are masked (***). */
  secretFields: ['password', 'newPassword', 'token', 'ssoToken'],
};
