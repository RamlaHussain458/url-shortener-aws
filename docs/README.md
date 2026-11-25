# Documentation

| Document | What it covers |
| --- | --- |
| [Development log](development-log.md) | Problems met while building the project and the changes made to fix them |
| [Deployment runbook](deployment.md) | How the application is deployed to AWS by hand |
| [Architecture decision records](adr/) | Short records explaining why important choices were made |

## Architecture decision records

| Record | Decision | Status |
| --- | --- | --- |
| [0001](adr/0001-random-short-codes.md) | Random Base62 short codes | Accepted |
| [0002](adr/0002-redirect-with-302.md) | Redirect with 302, not 301 | Accepted |
| [0003](adr/0003-free-tier-aws-architecture.md) | Low-cost AWS architecture for a student account | Accepted, implementation planned |
| [0004](adr/0004-singleton-testcontainer.md) | One shared MySQL Testcontainer for all integration tests | Accepted |
| [0005](adr/0005-frontend-behind-nginx.md) | React frontend served by nginx with a reverse proxy | Accepted |
| [0006](adr/0006-terraform-remote-state-in-s3.md) | Terraform remote state in S3 with native locking | Accepted |
| [0007](adr/0007-network-design.md) | Public web subnet, private database subnets, no NAT gateway | Accepted |
| [0008](adr/0008-compute-database-and-secrets.md) | Compute, database and secrets choices | Accepted |
| [0009](adr/0009-deployment-with-compose-on-ec2.md) | Deploy with Docker Compose on EC2, images from ECR | Accepted |

The architecture diagrams are in the main [README](../README.md).
