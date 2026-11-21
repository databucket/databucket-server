# Databucket

Open-source test data management: one place where teams store, share and reserve test data, so parallel tests never
fight over the same records. A REST API with client libraries for Java, Python and JavaScript, a web application for
testers, full history of changes, and an MCP server for AI agents.

* Website: https://databucket.pl
* Source: https://github.com/databucket/databucket-server
* Documentation: https://github.com/databucket/databucket-server/wiki

## Tags

* `latest` - the latest release
* `3.20.1`, `3.20`... - a release, or the latest patch of a minor version

Images for `linux/amd64` and `linux/arm64`. The image holds the jar of the GitHub release, run with Java 17
(Amazon Corretto) as user 1000, on port 8080.

## Quick start

Databucket needs PostgreSQL (developed and tested on 18). With Docker Compose:

```yaml
services:
  postgres:
    image: postgres:18-alpine
    environment:
      POSTGRES_DB: databucket
      POSTGRES_USER: databucket
      POSTGRES_PASSWORD: change-me
    volumes:
      - pgdata:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U databucket -d databucket"]
      interval: 5s
      retries: 10

  databucket:
    image: databucket/databucket-server:latest
    depends_on:
      postgres:
        condition: service_healthy
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/databucket
      SPRING_DATASOURCE_USERNAME: databucket
      SPRING_DATASOURCE_PASSWORD: change-me
      # signs the sign-in tokens: a random text of at least 32 characters, e.g. openssl rand -base64 48
      JWT_SECRET: change-me-to-a-random-text-of-at-least-32-characters

volumes:
  pgdata:
```

`docker compose up`, open http://localhost:8080 and sign in as `super` / `super` - change this password right away
(the user menu → Account). Then **Management → Projects**: create a project and import the **DEMO** dataset to look around.

**`JWT_SECRET` is required**: Databucket doesn't start without its own secret of at least 32 characters (since 3.21.0). Whoever knows it can sign in as any user, so keep it secret and don't reuse the examples.

## Configuration

Every setting is an environment variable (Spring Boot names), e.g.:

| Variable | |
|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | the PostgreSQL database (empty at the first start) |
| `JWT_SECRET` | signs the sign-in tokens, at least 32 characters |
| `JWT_EXPIRE_HOURS` | how long a sign-in is valid, 24 by default |
| `SERVER_SERVLET_CONTEXT_PATH` | e.g. `/databucket` to serve under a path |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_FROM` | the SMTP server for e-mails (sign-up, forgotten password) |
| `DATABUCKET_MCP_ENABLED` | `true` turns on the MCP server for AI agents at `/mcp` |
| `SSO_ENABLED`, `SSO_PROVIDER`, `SSO_ISSUER_URI`, `SSO_CLIENT_ID`, `SSO_CLIENT_SECRET` | single sign-on (OpenID Connect) |

All settings: [Installation](https://github.com/databucket/databucket-server/wiki/Installation),
[Single sign-on](https://github.com/databucket/databucket-server/wiki/Single-sign-on). A complete local setup with
single sign-on (Keycloak) and a mail catcher is the
[docker compose of the repository](https://github.com/databucket/databucket-server/tree/master/docker).

## Upgrading

Pull the new image and start it on the same database; the [Upgrading](https://github.com/databucket/databucket-server/wiki/Installation#upgrading)
table lists the releases that need a manual step.

License: Apache 2.0.
