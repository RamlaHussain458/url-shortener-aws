# Terraform keeps a record of what it built (the "state") in a private S3 bucket,
# so it is safe, versioned, and not stored in Git.
# Replace the bucket name with the one you created in infra/README.md (step 2).
terraform {
  backend "s3" {
    bucket       = "ramla-urlshortener-tfstate-x7k2"
    key          = "url-shortener/terraform.tfstate"
    region       = "ap-south-1"
    encrypt      = true
    use_lockfile = true
  }
}
