# Documentation

| Document | What it covers |
| --- | --- |
| [Development log](development-log.md) | Problems met while building the project and the changes made to fix them |
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

The architecture diagrams are in the main [README](../README.md).
