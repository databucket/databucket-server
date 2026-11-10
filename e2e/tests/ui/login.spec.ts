import { test, expect } from '../../fixtures';
import { LoginPage } from '../../pages/LoginPage';
import { ProjectPage } from '../../pages/ProjectPage';
import { screenshot } from '../../support/screenshot';

test.describe('Login page', () => {
  test('a member signs in and sees the buckets of his project', async ({ project, page }) => {
    await project.createBucket('orders');
    const login = new LoginPage(page);
    await login.open();

    await login.signIn(project.users.member);

    await expect(page).toHaveURL(new RegExp(`/project/${project.id}$`));
    await expect(new ProjectPage(page).bucket('orders')).toBeVisible();
  });

  test('a wrong password keeps the user on the login page', async ({ project, page }) => {
    const login = new LoginPage(page);
    await login.open();

    await login.signIn({ username: project.users.member.username, password: 'wrong password' });

    await expect(page.getByText(/bad credentials/i)).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);
    await screenshot(page, 'error message');
  });
});
