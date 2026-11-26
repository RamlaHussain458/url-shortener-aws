# Monitoring

Monitoring uses Amazon CloudWatch, which needs no extra servers and no changes to the application.

## What is collected

| What | Where | Details |
| --- | --- | --- |
| Container logs | CloudWatch Logs, groups `/url-shortener/app` and `/url-shortener/web` | Kept for 7 days. The log groups outlive the server, so logs remain after it is destroyed. |
| Server metrics | CloudWatch Metrics (EC2) | CPU, network and status checks. Memory is not collected, because that needs the CloudWatch agent. |
| Database metrics | CloudWatch Metrics (RDS) | CPU, connections and free storage |
| Application errors | Custom metric `UrlShortener/AppErrors` | A metric filter counts log lines containing `ERROR` |

## Alarms

All alarms email the address set in `terraform.tfvars` through an SNS topic. The subscription must be confirmed once, using the link in the first email from AWS.

| Alarm | Triggers when |
| --- | --- |
| `url-shortener-web-status-check-failed` | The server fails an EC2 status check for 2 minutes |
| `url-shortener-web-cpu-high` | Server CPU is above 80 percent for 10 minutes |
| `url-shortener-db-cpu-high` | Database CPU is above 80 percent for 10 minutes |
| `url-shortener-db-storage-low` | The database has less than 2 GiB of free storage |
| `url-shortener-app-errors` | The application logs 5 or more ERROR lines within 5 minutes |

The server and database alarms and the dashboard are removed together with the server and database, and recreated by the next `terraform apply`.

## Dashboard

The dashboard `url-shortener` (CloudWatch, Dashboards) shows server and database health, the error count and the latest application log lines.

## Try it

- **Logs:** CloudWatch, Log groups, `/url-shortener/app`, open the newest log stream.
- **Alarm email:** the command below puts an alarm into the ALARM state, which sends the email. It returns to OK by itself within a few minutes.
  ```
  aws cloudwatch set-alarm-state --alarm-name "url-shortener-app-errors" --state-value ALARM --state-reason "Testing the email notification"
  ```

## Limits

- Memory and disk usage of the server are not monitored.
- There is no check that calls the website from outside. A failed container would show up in the logs and error alarm, not as a separate uptime alarm.
- T3 instances run in unlimited CPU mode by default, so sustained high CPU can add a small charge. The CPU alarm is the warning for that.
