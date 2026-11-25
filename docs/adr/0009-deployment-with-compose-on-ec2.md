# 0009. Deploy with Docker Compose on EC2, images from ECR

**Status:** Accepted

**Context:** The application is two containers (API and web) plus a database. Options were ECS Fargate, Kubernetes, or Docker Compose on a single EC2 instance. Fargate and load balancers cost money every hour, and the account runs on limited credits.

**Decision:** Run both containers with Docker Compose on one EC2 instance. Images are built outside the server, pushed to private ECR repositories with a unique tag per Git commit, and pulled by the server. A deploy script reads the database settings from Parameter Store, writes them to a root-only `.env` file, and starts the stack. The compose file is downloaded from the Git repository at the chosen branch or tag.

**Consequences:**
- It is simple, cheap and easy to explain, and the same script can later be run by the CI/CD pipeline through Systems Manager.
- There is a single server and no automatic failover, and a deployment briefly restarts the containers.
- The server trusts whatever the chosen Git reference contains, so protecting `main` and using tags matters.
- A move to ECS Fargate behind a load balancer is the natural production step.
