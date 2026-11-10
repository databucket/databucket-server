import { test, expect } from '../../fixtures';
import { API_V2 } from '../../support/api';

const BUCKET = 'orders';
const DATA = `/api/bucket/${BUCKET}`;

test.describe('Data history', () => {
  test.beforeEach(async ({ project }) => {
    await project.createBucket(BUCKET, { history: true });
  });

  test('records the changes of a row', async ({ project }) => {
    const robot = await project.as('robot');
    const row = await robot.post(DATA, { headers: API_V2, data: { properties: { status: 'NEW' } }, expectStatus: 201 });
    await robot.put(`${DATA}/${row.body.id}`, { headers: API_V2, data: { propertiesToSet: { '$.status': 'PAID' } }, expectStatus: 200 });

    const history = await robot.get(`${DATA}/${row.body.id}/history`, { headers: API_V2, expectStatus: 200 });

    expect(history.body).toHaveLength(2);
    expect(history.body.every((entry: any) => entry.modified_by === project.users.robot.username)).toBe(true);
  });

  test('searches the history of the rows matching the rules', async ({ project }) => {
    const robot = await project.as('robot');
    const paid = await robot.post(DATA, { headers: API_V2, data: { properties: { status: 'NEW', customer: 'Alice' } }, expectStatus: 201 });
    await robot.post(DATA, { headers: API_V2, data: { properties: { status: 'NEW', customer: 'Bob' } }, expectStatus: 201 });
    await robot.put(`${DATA}/${paid.body.id}`, { headers: API_V2, data: { propertiesToSet: { '$.status': 'PAID' } }, expectStatus: 200 });

    const search = await robot.post(`${DATA}/history/get?limit=10&properties=true`, {
      headers: API_V2,
      data: { logic: { '==': [{ var: '$.customer' }, 'Alice'] } },
      expectStatus: 200,
    });

    expect(search.body.total).toBe(2);
    expect(search.body.history.every((entry: any) => entry.dataId === paid.body.id)).toBe(true);
  });
});
