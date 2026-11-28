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
- **Note:** Environment variables take precedence over `.env`, so a stray `DB_*` variable on a machine can cause the same symptom. The same rule applies to AWS credentials: environment variables override an AWS CLI profile, so I select the profile with `AWS_PROFILE=urlshortener`.

## 3. Integration tests failed on GitHub Actions

- **What happened:** Three integration tests failed on CI with `Connection refused` and HikariPool timeouts.
- **Cause:** Each test class started its own MySQL container and stopped it afterwards, while Spring cached the application context between classes. The cached context still pointed at the first container, which was already stopped.
- **Change made:** The shared test base class now starts one MySQL container in a static block and reuses it for the whole test run (see [ADR 0004](adr/0004-singleton-testcontainer.md)). CI went green after this change. As a result, the integration tests now fail when Docker is not running instead of being skipped, and the README says so.

## 4. Keeping AWS cost under control

- **Symptom:** The AWS dashboard showed about $20 of credit spent in a month on a student free plan.
- **Cause:** Leftover resources from earlier projects (compute, databases, networking) kept running.
- **Fix:** Review Cost Explorer grouped by usage type, delete unused resources, add a budget alert, and design this project to avoid NAT gateways, load balancers and Fargate (see [ADR 0003](adr/0003-free-tier-aws-architecture.md)).
- **Lesson:** Cost is a design constraint. Set budget alerts before building anything, and check Cost Explorer regularly.

## 5. PowerShell broke the ECR login

- **What happened:** Pushing images from PowerShell failed at the Docker login step, and ECR answered `400 Bad Request`.
- **Cause:** In Windows PowerShell 5.1, piping the output of `aws ecr get-login-password` straight into `docker login --password-stdin` can corrupt the password.
- **Change made:** `scripts/push-images.ps1` now runs the login through `cmd /c`, so cmd.exe handles the pipe. The script also checks the exit code of each step and checks that the ECR repositories exist before the slow image build.
- **Lesson:** In scripts, check the exit code after every native command, and do not assume a pipe passes bytes through unchanged.

## 6. GitHub Actions could not sign in to AWS with OIDC

- **What happened:** The Deploy workflow could not get AWS credentials through OIDC. The error was: `Could not assume role with OIDC: Not authorized to perform sts:AssumeRoleWithWebIdentity`.
- **Cause:** The IAM role's trust policy compares the `sub` claim in the token that GitHub issues. For this repository GitHub names the repository with numeric IDs, in the form `owner@ownerId/repo@repoId`, not the plain `owner/repo` that I had written.
- **Change made:** I temporarily added a workflow step that printed the token's claims, copied the exact `sub` value into the Terraform variable `github_oidc_subject_repo`, and applied. After the sign-in worked I removed the debug step in a separate pull request, because it printed identity details into a public log.
- **Lesson:** Do not guess identity claims. Print them once, copy them, and remove the debugging.

## 7. A Compose file in .github/workflows was treated as a workflow

- **What happened:** I committed `docker-compose.prod.yml` into `.github/workflows/` by mistake. GitHub tried to run it as a workflow and the run failed with: `Unexpected value 'services'` and `Required property is missing: jobs`.
- **Cause:** GitHub treats every YAML file in `.github/workflows/` as a workflow definition, and a workflow needs an `on:` trigger and a `jobs:` section. A Compose file has a `services:` section instead, so it failed validation.
- **Change made:** Moved the file to `deploy/`, updated the paths that referenced it, and redeployed. The deploy completed.
- **Lesson:** `.github/workflows/` is for workflows only. Keep other YAML files in a separate folder.

## 8. The Copy button failed on the EC2 public IP

- **What happened:** The Copy button worked on `localhost` but did nothing on the deployed site, which is served over plain HTTP.
- **Cause:** Browsers expose the modern Clipboard API (`navigator.clipboard`) only in a secure context, which means HTTPS or `localhost`. Over HTTP on a public IP, it is not available.
- **Change made:** `App.jsx` uses `navigator.clipboard` when it is available and falls back to a hidden text field with `document.execCommand('copy')` when it is not. If both fail, the page shows a message so the visitor can copy the link by hand.
- **Lesson:** Features that need a secure context behave differently on `localhost` and on a real HTTP address. HTTPS is the proper long-term fix.