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
npm run report                    # the Allure report of the last run (npm run report:playwright: the Playwright one)
```

Another Databucket: `BASE_URL=http://localhost:8080 SUPER_USER=... SUPER_PASSWORD=... npm test`.

Handy while writing tests: `npx playwright test --ui` (watch mode with the browser), `npx playwright test -g "searches"`
(tests by name), `--headed` (see the browser).

## How the tests are isolated

Every test gets its own project (fixture `project`): a SUPER admin creates it with four users, ADMIN, MEMBER,
ROBOT and AGENT (`project.users.admin` etc.), and removes it after the test (cleared and deleted; the users are disabled, as
users can't be deleted). Tests don't see each other's data and run in parallel. The test data is created through the
API: `project.createBucket(...)`, `project.createView(...)`, `project.quietly('robot')` for calls left out of the report.

## Structure

| Path | |
|---|---|
| `tests/api/` | API tests (project `api`) |
| `tests/ui/` | GUI tests in Chromium (project `ui`) |
| `pages/` | page objects of the GUI |
| `support/api.ts` | REST client: every call is an Allure step with the request and the response |
| `support/report-config.ts` | what the report shows: response headers (`date`, `x-request-id`...), masked headers and fields |
| `support/project.ts` | the test project with its users, buckets and views |
| `fixtures.ts` | `test` and `expect` to import in the tests |

## The report

[Allure](https://allurereport.org) in the classic Allure 2 layout (Allure 3 CLI with its `allure2` plugin, `allure-playwright`;
no Java needed): `npm run report` generates `allure-report/index.html` (one file, opens from the disk) and opens it. In GitHub Actions it is the `allure-report` artifact of the run.

- API calls are steps such as `POST /api/bucket/orders/get → 200` with the request and the response attached as one
  HTML view (`support/http-view.ts`): method, URL, status and time, then the request and the response with their
  headers (collapsed) and highlighted JSON bodies. The view is named by the `name` option of the call, e.g.
  `api.post(path, { name: 'GET data by rules', ... })`, "request / response" otherwise. A Copy button copies the
  request and the response, with or without headers, as plain text (to paste e.g. into a code block in Slack or
  Mattermost). Tokens
  and passwords are masked, also in what is copied.
- GUI tests: the actions of the page objects are steps ("Open bucket orders", "Search \"Bob\""); a screenshot at the
  end of every test (and named ones from `screenshot(page, '...')`), the Playwright trace of every test (download it and
  open it at https://trace.playwright.dev or with `npx playwright show-trace`) and a video of the failed ones.

The Playwright HTML report is still written (`playwright-report/`, `npm run report:playwright`), with its built-in trace viewer.

## Selectors

Prefer roles and accessible names (`getByRole('button', { name: 'Add' })`), as a user sees the page. Where the
frontend has nothing usable, add a `data-testid` to the component rather than relying on CSS classes.
