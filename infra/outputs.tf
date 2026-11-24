output "region" {
  description = "AWS region used by this configuration."
  value       = var.aws_region
}

output "project_name" {
  description = "Project name used in tags."
  value       = var.project_name
}

output "vpc_id" {
  description = "ID of the VPC."
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "IDs of the public subnets."
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "IDs of the private subnets."
  value       = aws_subnet.private[*].id
}

output "web_security_group_id" {
  description = "ID of the web server security group."
  value       = aws_security_group.web.id
}

output "db_security_group_id" {
  description = "ID of the database security group."
  value       = aws_security_group.db.id
}

output "web_public_ip" {
  description = "Public address of the web server."
  value       = aws_eip.web.public_ip
}

output "web_instance_id" {
  description = "ID of the web server instance (used for Session Manager)."
  value       = aws_instance.web.id
}

output "ecr_repository_urls" {
  description = "Addresses of the container registries."
  value       = { for key, repo in aws_ecr_repository.app : key => repo.repository_url }
}

output "db_endpoint" {
  description = "Private address of the database."
  value       = aws_db_instance.main.address
}
