import { defineConfig, devices } from '@playwright/test';
import { env } from './support/env';

export default defineConfig({
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: env.baseUrl,
  },
  projects: [
    {
      name: 'api',
      testDir: './tests/api',
    },
    {
      name: 'ui',
      testDir: './tests/ui',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1440, height: 900 },
        // in the report: a screenshot of the end of every test, the trace (page snapshots, network, console) of every
        // test, a video of the failed ones
        screenshot: 'on',
        trace: 'on',
        video: 'retain-on-failure',
      },
    },
  ],
});
