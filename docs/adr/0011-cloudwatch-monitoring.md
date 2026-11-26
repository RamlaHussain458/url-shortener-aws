# 0011. Monitoring with CloudWatch

**Status:** Accepted

**Context:** The deployment needs logs, alarms and a way to see its health. Prometheus and Grafana were considered. They would need two more containers on a 2 GiB server, a metrics endpoint kept off the public site, a protected Grafana login, and extra tooling to read database metrics. Their data would also be lost with each destroyed server.

**Decision:** Use CloudWatch. Containers log with the Docker `awslogs` driver to log groups with a short retention. Alarms for the server, the database and application errors send email through SNS. A dashboard shows the main graphs. Everything is defined in Terraform, and the alert email address is a variable kept out of Git.

**Consequences:**
- There are no extra servers, no memory pressure on the web server, and no change to the application.
- Logs and metrics are stored by AWS and outlive the server.
- The server's role needs permission to write to the log groups, so Terraform must be applied before a deployment that uses them.
- Memory and disk metrics are not collected without the CloudWatch agent.
- Prometheus and Grafana remain a good fit for application-level metrics and load-test comparisons, and can be added later in the local Docker Compose setup.
