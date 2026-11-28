# 0003. Low-cost AWS architecture for a student account

**Status:** Accepted and implemented (see ADRs 0007 to 0011 for the details)

**Context:** The project runs on an AWS free plan with limited credits that expire. NAT gateways, load balancers, Fargate and ElastiCache all have hourly costs.

**Decision:** Run a single small EC2 instance with Docker and a single-AZ RDS MySQL database, with no NAT gateway and no load balancer. Define everything in Terraform so the expensive parts can be destroyed and recreated between work sessions.

**Consequences:**
- Cost stays low only because the server, the database and the public IP address are removed between work sessions. While they exist they cost roughly $1.30 a day (my own estimate, not an AWS quote).
- The design is not highly available. A production version would use an ALB, private subnets, multi-AZ RDS and ECS Fargate.
- Destroying the database deletes its data, so the demo starts empty every session.
- Everything must be reproducible with `terraform apply`, and screenshots and a demo are kept as evidence once the account expires.