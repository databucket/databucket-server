import { test, expect } from '../../fixtures';
import { API_V2 } from '../../support/api';

const BUCKET = 'orders';
const DATA = `/api/bucket/${BUCKET}`;

test.describe('Data API v2', () => {
  test.beforeEach(async ({ project }) => {
    await project.createBucket(BUCKET);
  });

  test('inserts a row and reads it by id', async ({ project }) => {
    const robot = await project.as('robot');

    const inserted = await robot.post(DATA, {
      headers: API_V2,
      data: { properties: { customer: 'Alice', status: 'NEW', total: 120.5 } },
      expectStatus: 201,
    });
    expect(inserted.body.id).toEqual(expect.any(Number));
    expect(inserted.body.properties).toEqual({ customer: 'Alice', status: 'NEW', total: 120.5 });
    expect(inserted.body.reserved).toBe(false);

    const read = await robot.get(`${DATA}/${inserted.body.id}`, { headers: API_V2, expectStatus: 200 });
    expect(read.body).toMatchObject({ id: inserted.body.id, properties: { customer: 'Alice' }, createdBy: project.users.robot.username });
  });

  test('finds rows by JsonLogic rules with paging, sorting and counting', async ({ project }) => {
    const robot = await project.as('robot');
    await robot.post(`${DATA}/multi`, {
      headers: API_V2,
      data: [
        { properties: { customer: 'Alice', status: 'NEW', total: 10 } },
        { properties: { customer: 'Bob', status: 'PAID', total: 20 } },
        { properties: { customer: 'Carol', status: 'NEW', total: 30 } },
        { properties: { customer: 'Dave', status: 'NEW', total: 40 } },
      ],
      expectStatus: 201,
    });
    const newOrders = { logic: { '==': [{ var: '$.status' }, 'NEW'] } };

    const count = await robot.post(`${DATA}/get?limit=0`, { headers: API_V2, data: newOrders, expectStatus: 200 });
    expect(count.body.total).toBe(3);

    const page = await robot.post(`${DATA}/get?limit=2&page=1&sort=desc(${encodeURIComponent('$.total')})`, {
      headers: API_V2,
      data: newOrders,
      expectStatus: 200,
    });
    expect(page.body.data.map((row: any) => row.properties.customer)).toEqual(['Dave', 'Carol']);
  });

  test('reserves free rows only once', async ({ project }) => {
    const robot = await project.as('robot');
    await robot.post(`${DATA}/multi`, {
      headers: API_V2,
      data: [{ properties: { customer: 'Alice' } }, { properties: { customer: 'Bob' } }],
      expectStatus: 201,
    });

    const first = await robot.post(`${DATA}/reserve?limit=1`, { headers: API_V2, data: {}, expectStatus: 200 });
    const second = await robot.post(`${DATA}/reserve?limit=5`, { headers: API_V2, data: {}, expectStatus: 200 });
    const third = await robot.post(`${DATA}/reserve?limit=1`, { headers: API_V2, data: {} });

    expect(first.body.data).toHaveLength(1);
    expect(second.body.data).toHaveLength(1);
    expect(second.body.data[0].id).not.toBe(first.body.data[0].id);
    expect([...first.body.data, ...second.body.data].every((row: any) => row.reserved && row.owner === project.users.robot.username)).toBe(true);
    expect(third.body.data ?? []).toHaveLength(0);
  });

  test('updates one property, keeping the others, and deletes rows', async ({ project }) => {
    const member = await project.as('member');
    const row = await member.post(DATA, { headers: API_V2, data: { properties: { status: 'NEW', customer: 'Alice' } }, expectStatus: 201 });

    await member.put(`${DATA}/${row.body.id}`, {
      headers: API_V2,
      data: { propertiesToSet: { '$.status': 'PAID' } },
      expectStatus: 200,
    });
    const updated = await member.get(`${DATA}/${row.body.id}`, { headers: API_V2, expectStatus: 200 });
    expect(updated.body.properties).toEqual({ status: 'PAID', customer: 'Alice' });

    await member.delete(`${DATA}/${row.body.id}`, { headers: API_V2, expectStatus: 200 });
    // a row that doesn't exist (any more) comes back as an empty list
    const deleted = await member.get(`${DATA}/${row.body.id}`, { headers: API_V2, expectStatus: 200 });
    expect(deleted.body).toEqual([]);
  });
});
