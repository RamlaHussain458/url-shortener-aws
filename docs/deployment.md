# Deployment runbook

The application is deployed to one AWS web server. There are two ways to deploy: automatically with the pipeline, or by hand. Both run the same `deploy/deploy.sh` on the server.

For how the pieces fit together and why, see [AWS deployment](aws-deployment.md).

## Overview

1. Terraform creates the network, the server, the database, the registries, the IAM roles, the stored parameters and the monitoring.
2. The two container images (API and web) are built and pushed to ECR, tagged with the Git commit.
3. `deploy/deploy.sh` runs on the server. It downloads `deploy/docker-compose.prod.yml`, reads the database settings from Systems Manager Parameter Store, logs in to ECR, pulls the images, starts the containers and waits for the health check.

`terraform apply` only creates the infrastructure. It does not deploy the application. Deploying is always a separate step.

## Start the infrastructure

The server, the database and the public IP address cost money every hour (roughly $1.30 a day for all three, my own estimate). Start them for a work session and remove them afterwards.

```powershell
cd infra
terraform apply
```

Review the plan before you confirm. After a targeted destroy you should see about 10 resources to add.

Wait a few minutes after it finishes. Before deploying, check two things:

1. **The server finished its first boot.** In a Session Manager session, `ls -a /opt/url-shortener` shows `.bootstrap-done`. The file name starts with a dot, so `ls` without `-a` does not list it.
2. **The server is registered with Systems Manager.** The pipeline needs this to send its command.

   ```powershell
   aws ssm describe-instance-information --region ap-south-1 --query "InstanceInformationList[].[InstanceId,PingStatus]" --output table
   ```

   You should see one instance with `Online`. If the list is empty, wait another minute or two and run it again.

## Stop the infrastructure

First preview what will be removed. This command changes nothing:

```powershell
terraform plan -destroy -target="aws_instance.web" -target="aws_eip.web" -target="aws_db_instance.main"
```

Then run the real destroy:

```powershell
terraform destroy -target="aws_instance.web" -target="aws_eip.web" -target="aws_db_instance.main"
```

The quotes around each `-target` value are required in PowerShell.

Terraform also removes everything that depends on those three, 10 resources in total: the server, the Elastic IP and its association, the database, the `db_host` parameter, the dashboard, and the four alarms that point at the server or database. The next `terraform apply` recreates them. The network, security groups, ECR repositories, IAM roles, log groups, SNS topic, the application error alarm and the other parameters stay.

**The database and its data are deleted.** There is no final snapshot, so links created in one session do not exist in the next.

Afterwards, open the EC2, Elastic IP and RDS pages for the Mumbai region in the console and check they are empty.

## Deploy with the pipeline

1. Start the infrastructure (above) and check that the server is booted and registered.
2. Publish a release on GitHub (Releases, Draft a new release, new tag such as `v0.2.0`, target `main`). Or run the **Deploy** workflow by hand from the Actions tab on the `main` branch.
3. The workflow signs in to AWS with OIDC, builds and pushes the images, finds the server, and runs the deploy script on it through Systems Manager.
4. Open `http://<web_public_ip>` (from `terraform output web_public_ip`). Type `http://` explicitly, because some browsers try HTTPS first when you type a bare IP address.

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
   By default the script downloads the Compose file from `main`. To use another branch, tag or commit, add `REF=<name>` before `bash`.
4. Open `http://<web_public_ip>` and create a short link.

## Troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| `terraform destroy` fails with "Too many command line arguments" | PowerShell split the `-target` value at the dot. Put quotes around each value |
| The workflow stops with "Expected exactly one running server" | The server does not exist yet, is still starting, or two servers are running. Run `terraform apply`, wait a few minutes, then run the workflow again |
| `ls /opt/url-shortener` does not show `.bootstrap-done` | The file name starts with a dot. Use `ls -a` |
| Session Manager opens in `/usr/bin` and you cannot save a file | Run `cd /tmp` first |
| The ECR login in `push-images.ps1` fails with `400 Bad Request` | Windows PowerShell 5.1 can corrupt a piped password. The script already logs in through `cmd /c`. Use the script, not a hand-typed pipe |
| `push-images.ps1` says the working tree has uncommitted changes | Commit or stash first, so the image tag matches the code |
| The browser does not load the site | Use `http://<ip>` explicitly. The site has no HTTPS |
| AWS commands use the wrong account or profile | Environment variables override profiles. Check which `AWS_` variables exist with `Get-ChildItem Env: \| Where-Object Name -like "AWS_*" \| Select-Object Name` (names only), and use `AWS_PROFILE=urlshortener` |
| Integration tests or image builds fail at startup | Docker Desktop is not running |

## Notes

- The site is served over HTTP only. HTTPS needs a domain name and a certificate, and is a possible later improvement.
- No SSH port is open. Shell access uses Session Manager.
- Container logs are sent to CloudWatch Logs (see [monitoring.md](monitoring.md)). Apply Terraform before the first deployment that uses them, because the server role needs permission to write to the log groups.
- Database settings are written to `/opt/url-shortener/.env` on the server, readable by root only. They are never stored in Git.
- The pipeline needs one repository secret, `AWS_DEPLOY_ROLE_ARN`, holding the ARN of the role created by Terraform (`terraform output github_deploy_role_arn`). The ARN only names the role. It is not a credential.