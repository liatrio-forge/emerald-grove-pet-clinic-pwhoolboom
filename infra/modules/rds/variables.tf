variable "project_name" {
  type = string
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "aws_account_id" {
  type        = number
  description = "AWS account ID"
}

variable "ecs_security_group_id" {
  type        = string
  description = "Security group ID of the ECS tasks; used to allow inbound PostgreSQL access to the RDS instance"
}

variable "db_name" {
  type        = string
  default     = "petclinic"
  description = "PostgreSQL database name"
}

variable "db_username" {
  type        = string
  default     = "petclinic"
  description = "PostgreSQL master username"
}

variable "platform_state_bucket" {
  type        = string
  default     = "terraform-state-ecs-landingzone-dev"
  description = "S3 bucket containing the platform Terraform state"
}

variable "platform_state_key" {
  type        = string
  default     = "ecs-application-landingzone/terraform.tfstate"
  description = "S3 key for the platform Terraform state file"
}

variable "platform_state_region" {
  type        = string
  default     = "us-east-1"
  description = "AWS region of the platform state S3 bucket"
}
