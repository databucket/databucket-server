import { test, expect } from '../../fixtures';
import { Api } from '../../support/api';

test.describe('Sign-in', () => {
  test('a user signs in to his project and gets a token', async ({ project, request }) => {
    const user = project.users.member;
    const anonymous = new Api(request, 'anonymous');

    const response = await anonymous.post('/api/public/signin', {
      data: { username: user.username, password: user.password, projectId: project.id },
      expectStatus: 200,
    });

    expect(response.body.token).toEqual(expect.any(String));
    expect(response.body.project).toMatchObject({ id: project.id, name: project.name });
    expect(response.body.roles).toContain('MEMBER');
  });

  test('a wrong password is refused', async ({ project, request }) => {
    const anonymous = new Api(request, 'anonymous');

    const response = await anonymous.post('/api/public/signin', {
      data: { username: project.users.member.username, password: 'wrong', projectId: project.id },
    });

    expect(response.status).toBe(401);
  });

  test('calls without a token are refused', async ({ request }) => {
    const anonymous = new Api(request, 'anonymous');

    const response = await anonymous.get('/api/buckets');

    expect(response.status).toBe(401);
  });
});

test.describe('Roles', () => {
  test('a MEMBER can not list the bucket settings', async ({ project }) => {
    const member = await project.as('member');

    const response = await member.get('/api/buckets');

    expect(response.status).toBe(403);
  });

  test('a ROBOT can not manage buckets', async ({ project }) => {
    const robot = await project.as('robot');

    const response = await robot.post('/api/buckets', { data: { name: 'not-allowed', icon: { name: 'Storage' } } });

    expect(response.status).toBe(403);
  });

  test('an ADMIN creates a bucket that the project members see', async ({ project }) => {
    const admin = await project.as('admin');
    await admin.post('/api/buckets', {
      data: { name: 'customers', icon: { name: 'Storage' }, history: true, roleId: 3 },
      expectStatus: 201,
    });

    const member = await project.as('member');
    const tree = await member.get('/api/session/access-tree', { expectStatus: 200 });

    expect(tree.body.buckets.map((b: any) => b.name)).toContain('customers');
  });
});
