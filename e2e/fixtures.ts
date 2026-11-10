import { test as base, expect } from '@playwright/test';
import { TestProject } from './support/project';

export interface Fixtures {
  /** A fresh project with its own ADMIN, MEMBER and ROBOT users, removed after the test. */
  project: TestProject;
}

export const test = base.extend<Fixtures>({
  project: [
    async ({ playwright, baseURL }, use) => {
      // a separate request context, so the users of the project don't share cookies with the test's own requests
      const request = await playwright.request.newContext({ baseURL });
      const project = await TestProject.create(request);
      await use(project);
      await project.dispose();
      await request.dispose();
    },
    { box: true, title: 'test project' },
  ],
});

export { expect };
