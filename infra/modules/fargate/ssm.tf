resource "aws_ssm_parameter" "anthropic_api_key" {
  name  = "/pet-clinic-pwhoolboom/${var.environment}/anthropic/api-key"
  type  = "SecureString"
  value = "PLACEHOLDER"

  lifecycle {
    ignore_changes = [value]
  }

  tags = {
    Name        = "/pet-clinic-pwhoolboom/${var.environment}/anthropic/api-key"
    Project     = var.project_name
    Environment = var.environment
  }
}
