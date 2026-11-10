import { expect, type Locator, type Page } from '@playwright/test';
import { step } from 'allure-js-commons';
import type { TestUser } from '../support/project';

export class LoginPage {
  constructor(private readonly page: Page) {}

  get username(): Locator {
    return this.page.getByRole('textbox', { name: 'Username' });
  }

  get password(): Locator {
    return this.page.getByRole('textbox', { name: 'Password' });
  }

  get loginButton(): Locator {
    return this.page.getByRole('button', { name: 'Login' });
  }

  async open(): Promise<void> {
    await step('Open the login page', async () => {
      await this.page.goto('/login');
      await expect(this.page.getByRole('heading', { name: 'Login' })).toBeVisible();
    });
  }

  async signIn(user: Pick<TestUser, 'username' | 'password'>): Promise<void> {
    await step(`Sign in as ${user.username}`, async () => {
      await this.username.fill(user.username);
      await this.password.fill(user.password);
      await this.loginButton.click();
    });
  }
}
