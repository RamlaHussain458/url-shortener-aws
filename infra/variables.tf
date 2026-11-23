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

variable "vpc_cidr" {
  description = "IP address range of the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "IP ranges of the public subnets, one per availability zone."
  type        = list(string)
  default     = ["10.0.0.0/24", "10.0.1.0/24"]
}

variable "private_subnet_cidrs" {
  description = "IP ranges of the private (database) subnets, one per availability zone."
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]
}
