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

variable "instance_type" {
  description = "EC2 instance type for the web server. t3.small has 2 GiB of memory, t3.micro has 1 GiB and costs less."
  type        = string
  default     = "t3.small"
}

variable "db_instance_class" {
  description = "RDS instance class for the MySQL database."
  type        = string
  default     = "db.t4g.micro"
}

variable "db_allocated_storage" {
  description = "Database storage in GiB."
  type        = number
  default     = 20
}

variable "db_name" {
  description = "Name of the MySQL database created inside the RDS instance."
  type        = string
  default     = "urlshortener"
}

variable "db_username" {
  description = "Database user the application connects as."
  type        = string
  default     = "urlshortener"
}

variable "ecr_images_to_keep" {
  description = "Number of most recent images kept in each container registry."
  type        = number
  default     = 5
}

variable "github_repository" {
  description = "GitHub repository (owner/name) that is allowed to deploy."
  type        = string
  default     = "RamlaHussain458/url-shortener-aws"
}

variable "create_github_oidc_provider" {
  description = "Create the GitHub identity provider. Set to false if the AWS account already has one."
  type        = bool
  default     = true
}

variable "github_oidc_subject_repo" {
  description = "How GitHub names this repository inside the OIDC token: owner@ownerId/repo@repoId. Copy it from the sub claim printed by the debug step."
  type        = string
  default     = "RamlaHussain458@185750586/url-shortener-aws@1466680013"
}