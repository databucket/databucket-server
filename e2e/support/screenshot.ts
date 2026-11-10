import { test, type Page } from '@playwright/test';

/** A screenshot of the page, attached to the report under the given name (next to the automatic end-of-test one). */
export async function screenshot(page: Page, name: string): Promise<void> {
  await test.info().attach(name, { body: await page.screenshot(), contentType: 'image/png' });
}
