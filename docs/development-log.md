# Development log

Problems met while building this project and the changes made to fix them.

## 1. Docker was not running

- **What happened:** `docker compose up -d db` failed with an error mentioning `dockerDesktopLinuxEngine`.
- **Cause:** Docker Desktop was not running, so the `docker` command had no engine to connect to.
- **Change made:** Started Docker Desktop and waited for the engine to be ready. The database container then started normally.

## 2. Access denied when the application first connected to MySQL

- **What happened:** The first run from IntelliJ failed with `Access denied for user 'urlshortener'`, although the database container was healthy.
- **Cause:** The startup log showed that the application was launched from the repository root, while the configuration looked for the `.env` file one folder above the working directory (`../.env`), which is outside the repository.
- **Change made:** `spring.config.import` in `application.yml` now lists both `./.env` and `../.env`. After this change the application connected and Flyway applied the first migration.
- **Note:** Environment variables take precedence over `.env`, so a stray `DB_*` variable on a machine can cause the same symptom.

## 3. Integration tests failed on GitHub Actions

- **What happened:** Three integration tests failed on CI with `Connection refused` and HikariPool timeouts.
- **Cause:** Each test class started its own MySQL container and stopped it afterwards, while Spring cached the application context between classes. The cached context still pointed at the first container, which was already stopped.
- **Change made:** The shared test base class now starts one MySQL container in a static block and reuses it for the whole test run (see [ADR 0004](adr/0004-singleton-testcontainer.md)). CI went green after this change. As a result, the integration tests now fail when Docker is not running instead of being skipped, and the README says so.

## 5. Keeping AWS cost under control

- **Symptom:** The AWS dashboard showed about $20 of credit spent in a month on a student free plan.
- **Cause:** Leftover resources from earlier projects (compute, databases, networking) kept running.
- **Fix:** Review Cost Explorer grouped by usage type, delete unused resources, add a budget alert, and design this project to avoid NAT gateways, load balancers and Fargate (see ADR 0003).
- **Lesson:** Cost is a design constraint. Set budget alerts before building
