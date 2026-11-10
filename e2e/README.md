# Databucket end-to-end tests

Tests of the REST API and the GUI of a running Databucket, with [Playwright](https://playwright.dev) and TypeScript.
They are not part of the Gradle build; GitHub Actions runs them on every pull request (`.github/workflows/e2e.yml`).

## Running

Start Databucket, e.g. the docker compose of this repository (http://databucket.localhost, user `super` / `super`):

```
cd docker && docker compose up -d --build
```

Then:

```
cd e2e
npm install
npx playwright install chromium   # once
npm test                          # everything; npm run test:api / npm run test:ui
npm run report                    # the HTML report of the last run
```

Another Databucket: `BASE_URL=http://localhost:8080 SUPER_USER=... SUPER_PASSWORD=... npm test`.

Handy while writing tests: `npx playwright test --ui` (watch mode with the browser), `npx playwright test -g "searches"`
(tests by name), `--headed` (see the browser).

## How the tests are isolated

Every test gets its own project (fixture `project`): a SUPER admin creates it with three users, ADMIN, MEMBER and
ROBOT (`project.users.admin` etc.), and removes it after the test (cleared and deleted; the users are disabled, as
users can't be deleted). Tests don't see each other's data and run in parallel. The test data is created through the
API: `project.createBucket(...)`, `project.createView(...)`, `project.quietly('robot')` for calls left out of the report.

## Structure

| Path | |
|---|---|
| `tests/api/` | API tests (project `api`) |
| `tests/ui/` | GUI tests in Chromium (project `ui`) |
| `pages/` | page objects of the GUI |
| `support/api.ts` | REST client: every call is a step in the report with the request and the response |
| `support/project.ts` | the test project with its users, buckets and views |
| `fixtures.ts` | `test` and `expect` to import in the tests |

## The report

`playwright-report/index.html`; in GitHub Actions the `playwright-report` artifact of the run.

- API calls are steps such as `e2e-...-robot: POST /api/bucket/orders/get`, with the request (method, URL, headers,
  JSON) and the response (status, time, JSON) attached. Tokens and passwords are masked.
- GUI tests: a screenshot at the end of every test (and named ones from `screenshot(page, '...')`), the trace of every
  test (open it from the report: page snapshots before and after every action, network, console) and a video of the
  failed ones.

## Selectors

Prefer roles and accessible names (`getByRole('button', { name: 'Add' })`), as a user sees the page. Where the
frontend has nothing usable, add a `data-testid` to the component rather than relying on CSS classes.
