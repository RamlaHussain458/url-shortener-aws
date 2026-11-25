# Manual deployment runbook

How the application is deployed to the AWS web server by hand. The pipeline in a later phase automates the same steps.

## Overview

1. Terraform creates the server, the database, the registries and the stored parameters.
2. `scripts/push-images.ps1` builds the two images on a developer machine and pushes them to ECR, tagged with the Git commit.
3. `deploy/deploy.sh` runs on the server. It downloads `deploy/docker-compose.prod.yml`, reads the database settings from Systems Manager Parameter Store, logs in to ECR, pulls the images, starts the containers and waits for the health check.

## Steps

1. Start the infrastructure: `terraform apply` in `infra/`. The database takes several minutes. Wait a few more minutes after the server starts, until `/opt/url-shortener/.bootstrap-done` exists on it.
2. From the repository root, with a clean Git tree and the AWS profile selected: `.\scripts\push-images.ps1`. Note the tag it prints.
3. Open a Session Manager session on the server (EC2 console, Connect, Session Manager).
4. Download and run the deploy script:
   ```
   curl -fsSL https://raw.githubusercontent.com/RamlaHussain458/url-shortener-aws/main/deploy/deploy.sh -o deploy.sh
   sudo IMAGE_TAG=<tag> REF=main bash deploy.sh
   ```
5. Open `http://<web_public_ip>` (from `terraform output web_public_ip`) and create a short link.

## Stopping to save credits

```
terraform destroy -target=aws_instance.web -target=aws_eip.web -target=aws_db_instance.main
```

## Notes

- The site is served over HTTP only. HTTPS needs a domain name and a certificate, and is a possible later improvement.
- No SSH port is open. Shell access uses Session Manager.
- Database settings are written to `/opt/url-shortener/.env` on the server, readable by root only. They are never stored in Git.
