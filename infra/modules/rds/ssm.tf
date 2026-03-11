resource "aws_ssm_parameter" "db_url" {
  name  = "/pet-clinic-pwhoolboom/${var.environment}/db/url"
  type  = "SecureString"
  value = "jdbc:postgresql://${aws_db_instance.main.endpoint}/${var.db_name}"

  tags = {
    Name        = "/pet-clinic-pwhoolboom/${var.environment}/db/url"
    Project     = var.project_name
    Environment = var.environment
  }
}

resource "aws_ssm_parameter" "db_username" {
  name  = "/pet-clinic-pwhoolboom/${var.environment}/db/username"
  type  = "SecureString"
  value = var.db_username

  tags = {
    Name        = "/pet-clinic-pwhoolboom/${var.environment}/db/username"
    Project     = var.project_name
    Environment = var.environment
  }
}

resource "aws_ssm_parameter" "db_password" {
  name  = "/pet-clinic-pwhoolboom/${var.environment}/db/password"
  type  = "SecureString"
  value = random_password.db_password.result

  tags = {
    Name        = "/pet-clinic-pwhoolboom/${var.environment}/db/password"
    Project     = var.project_name
    Environment = var.environment
  }
}
