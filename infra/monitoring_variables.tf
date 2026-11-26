variable "alert_email" {
  description = "Email address that receives alarm notifications. Set it in terraform.tfvars, which is not committed."
  type        = string

  validation {
    condition     = can(regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", var.alert_email))
    error_message = "Enter a valid email address."
  }
}

variable "log_retention_days" {
  description = "How many days container logs are kept in CloudWatch."
  type        = number
  default     = 7
}
