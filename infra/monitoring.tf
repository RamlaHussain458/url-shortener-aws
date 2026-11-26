# Logs: both containers send their output to CloudWatch Logs. The log groups are not tied to the
# server, so the logs stay available after the server is destroyed (and expire after a few days).
resource "aws_cloudwatch_log_group" "containers" {
  for_each = toset(["app", "web"])

  name              = "/${var.project_name}/${each.key}"
  retention_in_days = var.log_retention_days
}

# The server's role may write to these two log groups, and nowhere else.
data "aws_iam_policy_document" "web_logs" {
  statement {
    sid       = "WriteContainerLogs"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = [for group in aws_cloudwatch_log_group.containers : "${group.arn}:*"]
  }
}

resource "aws_iam_role_policy" "web_logs" {
  name   = "${var.project_name}-web-logs"
  role   = aws_iam_role.web.id
  policy = data.aws_iam_policy_document.web_logs.json
}

# Counts the log lines that contain the word ERROR, so an alarm can react to them.
resource "aws_cloudwatch_log_metric_filter" "app_errors" {
  name           = "${var.project_name}-app-errors"
  log_group_name = aws_cloudwatch_log_group.containers["app"].name
  pattern        = "\"ERROR\""

  metric_transformation {
    name          = "AppErrors"
    namespace     = "UrlShortener"
    value         = "1"
    default_value = "0"
    unit          = "Count"
  }
}

# Alarms send their messages to this topic, which forwards them to your email address.
# AWS emails a confirmation link first, and notifications only arrive after it is clicked.
resource "aws_sns_topic" "alerts" {
  name = "${var.project_name}-alerts"
}

resource "aws_sns_topic_subscription" "alert_email" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# Alarms for the server and the database are removed together with them when they are destroyed.
resource "aws_cloudwatch_metric_alarm" "web_status_check" {
  alarm_name          = "${var.project_name}-web-status-check-failed"
  alarm_description   = "The web server failed an EC2 status check."
  namespace           = "AWS/EC2"
  metric_name         = "StatusCheckFailed"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 2
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    InstanceId = aws_instance.web.id
  }
}

resource "aws_cloudwatch_metric_alarm" "web_cpu_high" {
  alarm_name          = "${var.project_name}-web-cpu-high"
  alarm_description   = "The web server CPU has been above 80 percent for 10 minutes."
  namespace           = "AWS/EC2"
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  threshold           = 80
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    InstanceId = aws_instance.web.id
  }
}

resource "aws_cloudwatch_metric_alarm" "db_cpu_high" {
  alarm_name          = "${var.project_name}-db-cpu-high"
  alarm_description   = "The database CPU has been above 80 percent for 10 minutes."
  namespace           = "AWS/RDS"
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  threshold           = 80
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "db_storage_low" {
  alarm_name          = "${var.project_name}-db-storage-low"
  alarm_description   = "The database has less than 2 GiB of free storage."
  namespace           = "AWS/RDS"
  metric_name         = "FreeStorageSpace"
  statistic           = "Minimum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 2147483648
  comparison_operator = "LessThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "app_errors" {
  alarm_name          = "${var.project_name}-app-errors"
  alarm_description   = "The application logged 5 or more ERROR lines within 5 minutes."
  namespace           = "UrlShortener"
  metric_name         = "AppErrors"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 5
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = var.project_name

  dashboard_body = templatefile("${path.module}/dashboard.json.tftpl", {
    region        = var.aws_region
    instance_id   = aws_instance.web.id
    db_identifier = aws_db_instance.main.identifier
    app_log_group = aws_cloudwatch_log_group.containers["app"].name
  })
}
