# Infrastructure (Terraform)

Infrastructure for the URL shortener is defined as code here. Nothing in this folder contains
credentials. Terraform reads them from your AWS CLI profile.

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