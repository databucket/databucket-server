import type { APIRequestContext } from '@playwright/test';
import { Api } from './api';
import { env, ROLE, TEST_PASSWORD, type RoleName } from './env';

export type UserKey = 'admin' | 'member' | 'robot' | 'agent';

const USER_ROLES: Record<UserKey, RoleName> = { admin: 'ADMIN', member: 'MEMBER', robot: 'ROBOT', agent: 'AGENT' };

export interface TestUser {
  username: string;
  password: string;
  role: RoleName;
}

/**
 * A project of its own for one test, with four users (ADMIN, MEMBER, ROBOT, AGENT) assigned to it. Tests don't see each
 * other's data, so they can run in parallel. Removed after the test (cleared and deleted; the users are disabled,
 * as users can't be deleted).
 */
export class TestProject {
  private readonly tokens = new Map<UserKey, string>();

  private constructor(
    private readonly request: APIRequestContext,
    private readonly superApi: Api,
    readonly id: number,
    readonly name: string,
    readonly users: Record<UserKey, TestUser>,
  ) {}

  static async create(request: APIRequestContext): Promise<TestProject> {
    const signIn = await new Api(request, env.superUser, undefined, true).post('/api/public/signin', {
      data: { username: env.superUser, password: env.superPassword },
      expectStatus: 200,
    });
    const superApi = new Api(request, env.superUser, signIn.body.token, true);

    // unique and within the limits: project name up to 30 characters, username up to 50
    const suffix = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
    const name = `e2e-${suffix}`;
    const project = await superApi.post('/api/manage/projects', {
      data: { name, description: 'Created by the e2e tests', enabled: true },
      expectStatus: 201,
    });

    const users = {} as Record<UserKey, TestUser>;
    for (const key of Object.keys(USER_ROLES) as UserKey[]) {
      const user: TestUser = { username: `${name}-${key}`, password: TEST_PASSWORD, role: USER_ROLES[key] };
      await superApi.post('/api/manage/users', {
        data: { username: user.username, enabled: true, rolesIds: [ROLE[user.role]], projectsIds: [project.body.id] },
        expectStatus: 201,
      });
      // a new user has no usable password: set a temporary one, then change it, so it isn't a forced change any more
      const temporary = `tmp-${suffix}`;
      await superApi.post('/api/manage/users/password/reset', {
        data: { username: user.username, password: temporary },
        expectStatus: 200,
      });
      const first = await new Api(request, user.username, undefined, true).post('/api/public/signin', {
        data: { username: user.username, password: temporary },
        expectStatus: 200,
      });
      await new Api(request, user.username, first.body.token, true).post('/api/users/password/change', {
        data: { username: user.username, password: temporary, newPassword: user.password },
        expectStatus: 200,
      });
      users[key] = user;
    }
    return new TestProject(request, superApi, project.body.id, name, users);
  }

  /** The user signed in to this project; his calls are reported. */
  async as(key: UserKey): Promise<Api> {
    return (await this.signedIn(key)).loud();
  }

  /** The user signed in to this project, not reported: for preparing data. */
  async quietly(key: UserKey): Promise<Api> {
    return this.signedIn(key);
  }

  /** A JWT of the user for this project. */
  async token(key: UserKey): Promise<string> {
    return (await this.signedIn(key)).token!;
  }

  private async signedIn(key: UserKey): Promise<Api> {
    const user = this.users[key];
    let token = this.tokens.get(key);
    if (!token) {
      const response = await new Api(this.request, user.username, undefined, true).post('/api/public/signin', {
        data: { username: user.username, password: user.password, projectId: this.id },
        expectStatus: 200,
      });
      token = response.body.token as string;
      this.tokens.set(key, token);
    }
    return new Api(this.request, key, token, true);
  }

  /** Creates a bucket every user of the project can use (access from AGENT up). */
  async createBucket(name: string, options: { history?: boolean; protectedData?: boolean } = {}): Promise<number> {
    const admin = await this.quietly('admin');
    const response = await admin.post('/api/buckets', {
      data: {
        name,
        icon: { name: 'Storage' },
        history: options.history ?? true,
        protectedData: options.protectedData ?? false,
        roleId: ROLE.AGENT,
      },
      expectStatus: 201,
    });
    return response.body.id;
  }

  /**
   * Creates a view of the bucket (every user from ROBOT up, all features) with columns for the given properties,
   * e.g. `{ customer: 'Customer' }` shows `$.customer` as "Customer". Without a view the GUI shows no data.
   */
  async createView(bucketId: number, name: string, properties: Record<string, string>): Promise<number> {
    const admin = await this.quietly('admin');
    const props = Object.entries(properties).map(([path, title]) => ({ path: `$.${path}`, type: 'string', uuid: `prop-${path}`, title }));
    const uuids = ['uuid_data_id', 'uuid_reserved', 'uuid_owner', ...props.map(p => p.uuid), 'uuid_modified_at'];
    const columns = await admin.post('/api/columns', {
      data: {
        name: `${name} columns`,
        configuration: {
          properties: props,
          columns: uuids.map(uuid => ({
            uuid,
            enabled: true,
            sorting: true,
            filtering: true,
            editable: uuid.startsWith('prop-') || uuid === 'uuid_reserved' ? 'always' : 'never',
          })),
        },
      },
      expectStatus: 201,
    });
    const view = await admin.post('/api/views', {
      data: {
        name,
        columnsId: columns.body.id,
        bucketsIds: [bucketId],
        roleId: ROLE.ROBOT,
        featuresIds: Array.from({ length: 16 }, (_, i) => i + 1),
        sort: 'desc(id)',
      },
      expectStatus: 201,
    });
    return view.body.id;
  }

  async dispose(): Promise<void> {
    await this.superApi.post(`/api/manage/projects/${this.id}/clear`, { expectStatus: 204 });
    await this.superApi.delete(`/api/manage/projects/${this.id}`, { expectStatus: 204 });
    for (const user of Object.values(this.users))
      await this.superApi.put('/api/manage/users', {
        data: { username: user.username, description: 'Created by the e2e tests', enabled: false, rolesIds: [ROLE[user.role]], projectsIds: [] },
      });
  }
}
