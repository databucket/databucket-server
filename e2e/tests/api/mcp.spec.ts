import { test, expect } from '../../fixtures';
import { API_V2, type Api } from '../../support/api';

// The MCP server for AI agents (/mcp, stateless streamable HTTP): JSON-RPC calls with the user's token.
const BUCKET = 'accounts';
const MCP_HEADERS = { Accept: 'application/json, text/event-stream' };

interface ToolResult {
  isError: boolean;
  /** The tool's answer: JSON parsed, or the message of an error. */
  value: any;
}

async function rpc(api: Api, method: string, params: object = {}, name?: string) {
  const response = await api.post('/mcp', {
    headers: MCP_HEADERS,
    data: { jsonrpc: '2.0', id: 1, method, params },
    expectStatus: 200,
    name,
  });
  return response.body;
}

async function callTool(api: Api, tool: string, args: object = {}): Promise<ToolResult> {
  const body = await rpc(api, 'tools/call', { name: tool, arguments: args }, `MCP ${tool}`);
  const text: string = body.result.content[0].text;
  const isError = body.result.isError === true;
  return { isError, value: isError ? text : JSON.parse(text) };
}

test.describe('MCP server', () => {
  test.beforeEach(async ({ project }) => {
    await project.createBucket(BUCKET, { history: true });
  });

  test('introduces itself and lists its tools', async ({ project }) => {
    const agent = await project.as('agent');

    const init = await rpc(agent, 'initialize', {
      protocolVersion: '2025-06-18',
      capabilities: {},
      clientInfo: { name: 'e2e', version: '1' },
    });
    expect(init.result.serverInfo.name).toBe('databucket');
    expect(init.result.instructions).toContain('list_buckets');

    const tools = await rpc(agent, 'tools/list');
    expect(tools.result.tools.map((tool: any) => tool.name).sort()).toEqual([
      'delete_data', 'describe_bucket', 'find_data', 'get_data', 'insert_data', 'list_buckets',
      'release_data', 'reserve_data', 'search_history', 'update_data',
    ]);
  });

  test('needs a token', async ({ playwright, baseURL }) => {
    const request = await playwright.request.newContext({ baseURL });
    const response = await request.post('/mcp', {
      headers: { ...MCP_HEADERS, 'Content-Type': 'application/json' },
      data: { jsonrpc: '2.0', id: 1, method: 'tools/list' },
    });
    expect(response.status()).toBe(401);
    await request.dispose();
  });

  test('an agent finds, reserves, changes and releases data', async ({ project }) => {
    const agent = await project.as('agent');

    const buckets = await callTool(agent, 'list_buckets');
    expect(buckets.value.map((bucket: any) => bucket.name)).toContain(BUCKET);

    const inserted = await callTool(agent, 'insert_data', {
      bucket: BUCKET,
      rows: [
        { properties: { login: 'alice', status: 'active' } },
        { properties: { login: 'bob', status: 'active' } },
        { properties: { login: 'carol', status: 'blocked' } },
      ],
    });
    expect(inserted.isError).toBe(false);
    expect(inserted.value).toHaveLength(3);

    const active = await callTool(agent, 'find_data', { bucket: BUCKET, rules: [['$.status', '==', 'active']], sort: 'asc($.login)' });
    expect(active.value.total).toBe(2);
    expect(active.value.data.map((row: any) => row.properties.login)).toEqual(['alice', 'bob']);

    const reserved = await callTool(agent, 'reserve_data', { bucket: BUCKET, rules: [['$.status', '==', 'active']] });
    expect(reserved.value).toMatchObject({ reserved: 1, available: 1 });
    const row = reserved.value.data[0];
    expect(row.owner).toBe(project.users.agent.username);

    const updated = await callTool(agent, 'update_data', { bucket: BUCKET, ids: [row.id], propertiesToSet: { '$.status': 'used' } });
    expect(updated.value.count).toBe(1);

    const released = await callTool(agent, 'release_data', { bucket: BUCKET, ids: [row.id] });
    expect(released.value.count).toBe(1);

    const history = await callTool(agent, 'search_history', { bucket: BUCKET, rules: [['id', '==', row.id]], properties: true });
    // inserted, reserved, changed, released
    expect(history.value.total).toBe(4);
    expect(history.value.history.every((entry: any) => entry.modifiedBy === project.users.agent.username)).toBe(true);
  });

  test('an agent can not remove data, a robot can', async ({ project }) => {
    const agent = await project.as('agent');
    const robot = await project.as('robot');
    const row = await robot.post(`/api/bucket/${BUCKET}`, { headers: API_V2, data: { properties: { login: 'dave' } }, expectStatus: 201 });

    const byAgent = await callTool(agent, 'delete_data', { bucket: BUCKET, ids: [row.body.id] });
    expect(byAgent.isError).toBe(true);
    expect(byAgent.value).toContain('ROBOT role');
    // the REST API refuses it too
    await agent.delete(`/api/bucket/${BUCKET}/${row.body.id}`, { headers: API_V2, expectStatus: 403 });

    const byRobot = await callTool(robot, 'delete_data', { bucket: BUCKET, ids: [row.body.id] });
    expect(byRobot.value.count).toBe(1);
  });

  test('explains mistakes to the agent', async ({ project }) => {
    const agent = await project.as('agent');

    const unknownBucket = await callTool(agent, 'find_data', { bucket: 'no-such-bucket' });
    expect(unknownBucket).toEqual({ isError: true, value: expect.stringContaining('list_buckets') });

    const sqlRule = await callTool(agent, 'find_data', { bucket: BUCKET, rules: ["properties->>'a' = '1'"] });
    expect(sqlRule).toEqual({ isError: true, value: expect.stringContaining('[left, operator, right]') });

    const unknownTag = await callTool(agent, 'insert_data', { bucket: BUCKET, rows: [{ properties: { a: 1 }, tagId: 999999 }] });
    expect(unknownTag).toEqual({ isError: true, value: expect.stringContaining('describe_bucket') });

    const nothing = await callTool(agent, 'find_data', { bucket: BUCKET, limit: 0 });
    expect(nothing.value.total).toBe(0);
  });
});
