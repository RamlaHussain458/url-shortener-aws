# Credentials are NOT written here. Terraform reads them from the AWS CLI profile
# selected with the AWS_PROFILE environment variable.
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = var.project_name
      ManagedBy = "terraform"
    }
  }
}
