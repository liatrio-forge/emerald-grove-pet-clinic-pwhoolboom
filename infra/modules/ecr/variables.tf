variable "project_name" {
  type        = string
  description = "Team/project name used in resource naming"
}

variable "environment" {
  type    = string
  default = "dev"
  description = "Deployment environment (e.g., dev, prod)"
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
  description = "AWS region"
}
