output "rds_endpoint" {
  value = aws_db_instance.main.endpoint
}

output "rds_security_group_id" {
  value = aws_security_group.rds.id
}

output "ssm_db_url_arn" {
  value = aws_ssm_parameter.db_url.arn
}

output "ssm_db_username_arn" {
  value = aws_ssm_parameter.db_username.arn
}

output "ssm_db_password_arn" {
  value = aws_ssm_parameter.db_password.arn
}
