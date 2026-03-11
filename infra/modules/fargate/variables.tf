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
  type = number
}

variable "container_port" {
  type        = number
  default     = 80
  description = "Port the container listens on"
}

variable "cpu" {
  type        = number
  default     = 256
  description = "ECS task CPU units (256, 512, 1024, 2048, 4096)"
}

variable "memory" {
  type        = number
  default     = 512
  description = "ECS task memory in MB"
}

variable "desired_count" {
  type        = number
  default     = 2
  description = "Desired number of running ECS tasks"
}

variable "health_check_path" {
  type        = string
  default     = "/"
  description = "ALB health check path"
}

variable "ecr_repository_url" {
  type        = string
  description = "Full URL of the ECR repository to pull the container image from (without tag)"
}

variable "environment_variables" {
  type = list(object({
    name  = string
    value = string
  }))
  default     = []
  description = "Environment variables to inject into the container"
}

variable "platform_state_bucket" {
  type    = string
  default = "terraform-state-ecs-landingzone-dev"
}

variable "platform_state_key" {
  type    = string
  default = "ecs-application-landingzone/terraform.tfstate"
}

variable "platform_state_region" {
  type    = string
  default = "us-east-1"
}
