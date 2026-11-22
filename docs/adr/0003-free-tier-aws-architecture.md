# 0003. Low-cost AWS architecture for a student account

**Status:** Accepted (implementation planned)

**Context:** The project runs on an AWS free plan with limited credits that expire. NAT gateways, load balancers, Fargate and ElastiCache all have hourly costs.

**Decision:** Plan a single small EC2 instance running Docker, a database on RDS or in a container, no NAT gateway and no load balancer, all defined in Terraform so it can be destroyed and recreated.

**Consequences:**
- Cost stays close to zero.
- The design is not highly available. A production version would use an ALB, private subnets, multi-AZ RDS and ECS Fargate.
- Everything must be reproducible with `terraform apply`, and screenshots and a demo are kept as evidence once the account expires.
