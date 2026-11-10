import { test, expect } from '../../fixtures';
import { ProjectPage } from '../../pages/ProjectPage';
import { API_V2 } from '../../support/api';
import { screenshot } from '../../support/screenshot';

const BUCKET = 'orders';

test.describe('Bucket data grid', () => {
  test.beforeEach(async ({ project }) => {
    const bucketId = await project.createBucket(BUCKET);
    await project.createView(bucketId, 'all orders', { customer: 'Customer', status: 'Status' });
    const robot = await project.quietly('robot');
    await robot.post(`/api/bucket/${BUCKET}/multi`, {
      headers: API_V2,
      data: [
        { properties: { customer: 'Alice', status: 'NEW' } },
        { properties: { customer: 'Bob', status: 'PAID' } },
        { properties: { customer: 'Carol', status: 'NEW' } },
      ],
      expectStatus: 201,
    });
  });

  test('shows the rows inserted through the API', async ({ project, page }) => {
    const ui = new ProjectPage(page);
    await ui.openAs(project, 'member');

    await ui.openBucket(BUCKET);

    await expect(ui.row('Alice')).toContainText('NEW');
    await expect(ui.row('Bob')).toContainText('PAID');
    await expect(ui.pagination).toHaveText('1-3 of 3');
  });

  test('searches the properties', async ({ project, page }) => {
    const ui = new ProjectPage(page);
    await ui.openAs(project, 'member');
    await ui.openBucket(BUCKET);

    await ui.search('Bob');

    await expect(ui.pagination).toHaveText('1-1 of 1');
    await expect(ui.row('Bob')).toBeVisible();
    await expect(ui.row('Alice')).toBeHidden();
  });

  test('adds a row that the API then finds', async ({ project, page }) => {
    const ui = new ProjectPage(page);
    await ui.openAs(project, 'member');
    await ui.openBucket(BUCKET);

    await ui.addRow({ Customer: 'Dave', Status: 'NEW' });

    await expect(ui.row('Dave')).toBeVisible();
    await screenshot(page, 'row added');
    const member = await project.as('member');
    const found = await member.post(`/api/bucket/${BUCKET}/get?limit=10`, {
      headers: API_V2,
      data: { logic: { '==': [{ var: '$.customer' }, 'Dave'] } },
      expectStatus: 200,
    });
    expect(found.body.data).toHaveLength(1);
    expect(found.body.data[0]).toMatchObject({ properties: { customer: 'Dave', status: 'NEW' }, createdBy: project.users.member.username });
  });
});
