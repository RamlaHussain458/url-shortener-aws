variable "aws_region" {
  description = "AWS region where the resources are created."
  type        = string
  default     = "ap-south-1"
}

variable "project_name" {
  description = "Short name used in tags and resource names."
  type        = string
  default     = "url-shortener"
}