# 0010. Deploy from GitHub Actions with OIDC and Systems Manager

**Status:** Accepted

**Context:** Deploying by hand works but is slow and easy to get wrong. A pipeline needs permission to push images and to run commands on the server. Storing long-lived AWS access keys in GitHub would create a secret that can leak and never expires.

**Decision:** The pipeline signs in with OpenID Connect. GitHub issues a short-lived token for each run, and AWS exchanges it for temporary credentials only if the token comes from this repository, on `main` or on a `v*` release tag. The IAM role can push to the two ECR repositories, find the server, and run a shell script on servers tagged with this project. It cannot change infrastructure. The deploy runs `deploy.sh` through Systems Manager Run Command, pinned to the exact Git commit being released, so no SSH port is needed.

**Consequences:**
- No AWS access keys exist in GitHub. The only stored value is the role's ARN.
- Releases deploy automatically when the infrastructure is running. When it is stopped to save credits, the workflow pushes the images and then stops with a clear message.
- The role can run commands on the server, so protecting `main` and release tags matters.
- Images are tagged with the short commit hash, and the workflow skips a build when that tag already exists.
- A reviewer approval step (GitHub Environments) and automatic rollbacks are possible later improvements.
