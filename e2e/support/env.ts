/** Where the tests run and how they get in. Everything can be overridden with environment variables. */
export const env = {
  /** Databucket under test; the docker compose of this repository by default. */
  baseUrl: process.env.BASE_URL ?? 'http://databucket.localhost',
  /** A SUPER admin: creates a separate project with its own users for every test. */
  superUser: process.env.SUPER_USER ?? 'super',
  superPassword: process.env.SUPER_PASSWORD ?? 'super',
};

/** The password of every user the tests create. */
export const TEST_PASSWORD = 'E2e-Passw0rd!';

export const ROLE = { SUPER: 1, ADMIN: 2, MEMBER: 3, ROBOT: 4 } as const;
export type RoleName = keyof typeof ROLE;
