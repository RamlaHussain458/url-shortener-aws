# Deployment runbook

The application is deployed to one AWS web server. There are two ways to deploy: automatically with the pipeline, or by hand. Both run the same `deploy/deploy.sh` on the server.

## Overview

1. Terraform creates the server, the database, the registries and the stored parameters.
2. The two container images (API and web) are built and pushed to ECR, tagged with the Git commit.
3. `deploy/deploy.sh` runs on the server. It downloads `deploy/docker-compose.prod.yml`, reads the database settings from Systems Manager Parameter Store, logs in to ECR, pulls the images, starts the containers and waits for the health check.

## Start and stop the infrastructure

The server, the database and the public IP address cost money every hour. Start them for a work session and remove them afterwards.

```powershell
cd infra
terraform apply
```

Wait a few minutes after it finishes. The server installs Docker when it first boots, and it is ready when `ls -a /opt/url-shortener` shows `.bootstrap-done`. The file name starts with a dot, so `ls` without `-a` does not list it.

```powershell
terraform destroy -target="aws_instance.web" -target="aws_eip.web" -target="aws_db_instance.main"
```

The quotes around each `-target` value are required in PowerShell.

## Deploy with the pipeline

1. Start the infrastructure (above) and wait for the server to finish booting.
2. Publish a release on GitHub (Releases, Draft a new release, new tag such as `v0.2.0`, target `main`). Or run the **Deploy** workflow by hand from the Actions tab.
3. The workflow signs in to AWS with OIDC, builds and pushes the images, finds the server, and runs the deploy script on it through Systems Manager.
4. Open `http://<web_public_ip>` (from `terraform output web_public_ip`).

If no server is running, the workflow still builds and pushes the images, then stops with a message asking you to run `terraform apply`.

## Deploy by hand

1. From the repository root, with a clean Git tree and the AWS profile selected: `.\scripts\push-images.ps1`. Note the tag it prints.
2. Open a Session Manager session on the server (EC2 console, Connect, Session Manager).
3. Move to a writable folder, download the script and run it:
   ```
   cd /tmp
   curl -fsSL https://raw.githubusercontent.com/RamlaHussain458/url-shortener-aws/main/deploy/deploy.sh -o deploy.sh
   sudo IMAGE_TAG=<tag> bash deploy.sh
   ```
4. Open `http://<web_public_ip>` and create a short link.

## Notes

- The site is served over HTTP only. HTTPS needs a domain name and a certificate, and is a possible later improvement.
- No SSH port is open. Shell access uses Session Manager.
- Database settings are written to `/opt/url-shortener/.env` on the server, readable by root only. They are never stored in Git.
- The pipeline needs one repository secret, `AWS_DEPLOY_ROLE_ARN`, holding the ARN of the role created by Terraform (`terraform output github_deploy_role_arn`).
