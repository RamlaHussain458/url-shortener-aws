# Infrastructure (Terraform)

Infrastructure for the URL shortener is defined as code here. Nothing in this folder contains
credentials. Terraform reads them from your AWS CLI profile.

For an explanation of what this creates and why, see [AWS deployment](../docs/aws-deployment.md).

## Requirements

- Terraform 1.11 or newer (needed for S3 native state locking)
- AWS CLI v2, with a profile configured: `aws configure --profile urlshortener`

In PowerShell, select the profile for the current terminal:

```powershell
$env:AWS_PROFILE = "urlshortener"
```

## One-time setup: the state bucket

Terraform stores its state in an S3 bucket. The bucket has to exist before Terraform can use it,
so it is created once with the AWS CLI. Bucket names are global, so add a random suffix.

```powershell
$BUCKET = "your-name-urlshortener-tfstate-x7k2"

aws s3api create-bucket --bucket $BUCKET --region ap-south-1 --create-bucket-configuration LocationConstraint=ap-south-1
aws s3api put-public-access-block --bucket $BUCKET --public-access-block-configuration "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
aws s3api put-bucket-versioning --bucket $BUCKET --versioning-configuration Status=Enabled
```

Then put the same bucket name in `backend.tf`. The bucket and the `region` in `backend.tf` must be in the same region (ap-south-1, Mumbai). New S3 buckets are encrypted by default.

## Everyday commands

```powershell
cd infra
terraform init       # once, and after changing providers or the backend
terraform fmt        # format the files
terraform validate   # check the configuration
terraform plan       # preview the changes
terraform apply      # create or change resources
terraform destroy    # remove everything Terraform created
```

Commit `.terraform.lock.hcl`. Never commit `.terraform/`, `*.tfstate` or `*.tfvars`. The root `.gitignore` already excludes them.

`terraform apply` creates the infrastructure. It does not deploy the application. Deploying is done by the Deploy workflow (see [`docs/deployment.md`](../docs/deployment.md)).

## Settings that are not committed

`alert_email` has no default value. Copy `terraform.tfvars.example` to `terraform.tfvars` and put your own address in it. `terraform.tfvars` is ignored by Git.

## Settings for GitHub Actions deployment

Terraform creates an IAM role that the Deploy workflow assumes through OpenID Connect. Three variables control it:

| Variable | What it does |
| --- | --- |
| `github_repository` | The repository (`owner/name`) that is allowed to deploy |
| `github_oidc_subject_repo` | How GitHub names the repository inside its OIDC token. For this repository that is `owner@ownerId/repo@repoId` with numeric IDs, not the plain name |
| `create_github_oidc_provider` | An AWS account can have only one GitHub identity provider. Set this to `false` if the account already has one |

The role's trust policy accepts only tokens from that repository, and only from the `main` branch or from tags that start with `v`.

If you fork the repository, change the first two variables to your own values. Do not guess the second one. I found mine by temporarily printing the claims of the OIDC token in a workflow run, then removed that step because it prints identity details into a public log (see entry 6 in the [development log](../docs/development-log.md)).

After `terraform apply`, store the role's ARN as the repository secret `AWS_DEPLOY_ROLE_ARN`:

```powershell
terraform output github_deploy_role_arn
```

The ARN only names the role. It is not a credential.

## Cost control

The server, the database and the public IP address cost money for every hour they exist (roughly $1.30 a day for the three together, my own estimate). The network, security groups, IAM roles, container registries, parameters and monitoring cost little or nothing, although I have not measured them separately. While working, remove only the expensive parts at the end of a session and bring them back at the start of the next one.

Preview first. This command changes nothing:

```powershell
terraform plan -destroy -target="aws_instance.web" -target="aws_eip.web" -target="aws_db_instance.main"
```

Then:

```powershell
# End of a session: remove the server, its IP address and the database
terraform destroy -target="aws_instance.web" -target="aws_eip.web" -target="aws_db_instance.main"

# Start of the next session: recreate whatever is missing
terraform apply
```

The quotes around each `-target` value are required in PowerShell. Without them it splits the name at the dot and fails with "Too many command line arguments".

Terraform also destroys everything that depends on those three, 10 resources in total: the server, the Elastic IP and its association, the database, the `db_host` parameter, the dashboard, and the four alarms that point at the server or database. `terraform apply` recreates them. The database is deleted without a final snapshot, so its data is lost.

At the very end of the project, run a plain `terraform destroy` to remove everything. The state
bucket is not managed by Terraform and has to be deleted by hand.