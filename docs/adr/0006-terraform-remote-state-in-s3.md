# 0006. Terraform remote state in S3 with native locking

**Status:** Accepted

**Context:** Terraform needs to remember what it has built. A local state file can be lost, cannot be shared with CI, and can contain sensitive values, so it must never be committed to Git.

**Decision:** Keep the state in a private, versioned S3 bucket and use the S3 backend's native lock file (`use_lockfile`), which needs Terraform 1.11 or newer. The bucket is created once with the AWS CLI because it must exist before Terraform can use it.

**Consequences:**
- No DynamoDB table is needed for locking, so there is one less service to pay for and manage.
- Versioning allows a previous state to be restored if something goes wrong.
- The bucket name is committed in `backend.tf`. It contains no secrets and no account number.
- The state bucket is not managed by Terraform itself, so it has to be deleted by hand when the project is torn down.
