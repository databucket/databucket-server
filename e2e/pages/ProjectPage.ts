import { expect, type Locator, type Page } from '@playwright/test';
import { step } from 'allure-js-commons';
import type { TestProject, UserKey } from '../support/project';

/** The project frontend: the bucket list in the drawer and the data grid of the open bucket. */
export class ProjectPage {
  constructor(private readonly page: Page) {}

  get main(): Locator {
    return this.page.getByRole('main');
  }

  get searchBox(): Locator {
    return this.main.getByRole('textbox', { name: 'Search' });
  }

  get addButton(): Locator {
    return this.main.getByRole('button', { name: 'Add' });
  }

  get pagination(): Locator {
    return this.main.getByText(/^\d+-\d+ of \d+$/);
  }

  /**
   * Opens the project as the user without going through the login form: the token is put where the login page
   * puts it. Tests of the login page itself use {@link LoginPage}.
   */
  async openAs(project: TestProject, key: UserKey): Promise<void> {
    await step(`Open the project as ${key} (${project.users[key].username})`, async () => {
      const token = await project.token(key);
      const session = {
        'user-name': project.users[key].username,
        token,
        'project-id': String(project.id),
        roles: rolesOf(project.users[key].role),
      };
      await this.page.addInitScript(values => {
        // only once, so a sign-out in the test isn't undone by a reload
        if (sessionStorage.getItem('e2e-session')) return;
        sessionStorage.setItem('e2e-session', '1');
        for (const [name, value] of Object.entries(values)) localStorage.setItem(name, value);
      }, session);
      await this.page.goto(`/project/${project.id}`);
    });
  }

  bucket(name: string): Locator {
    return this.page.getByRole('button', { name, exact: true });
  }

  async openBucket(name: string): Promise<void> {
    await step(`Open bucket ${name}`, async () => {
      await this.bucket(name).click();
      await expect(this.page.getByRole('tab', { name, selected: true })).toBeVisible();
    });
  }

  /** A data row of the grid containing the text. */
  row(text: string): Locator {
    return this.main.getByRole('row').filter({ hasText: text });
  }

  async search(text: string): Promise<void> {
    await step(`Search "${text}"`, () => this.searchBox.fill(text));
  }

  /** Adds a row with the grid's inline editor; fields by their column titles. */
  async addRow(values: Record<string, string>): Promise<void> {
    await step(`Add a row ${JSON.stringify(values)}`, async () => {
      await this.addButton.click();
      const editor = this.main.getByRole('row').filter({ has: this.page.getByRole('button', { name: 'Save' }) });
      for (const [column, value] of Object.entries(values)) await editor.getByRole('textbox', { name: column }).fill(value);
      await editor.getByRole('button', { name: 'Save' }).click();
      await expect(editor).toBeHidden();
    });
  }
}

function rolesOf(role: string): string {
  const order = ['ROBOT', 'MEMBER', 'ADMIN', 'SUPER'];
  return order.slice(0, order.indexOf(role) + 1).reverse().join(',');
}
