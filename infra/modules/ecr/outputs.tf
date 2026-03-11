output "repository_url" {
  value       = aws_ecr_repository.pet-clinic-pwhoolboom.repository_url
  description = "ECR repository URL"
}

output "repository_name" {
  value       = aws_ecr_repository.pet-clinic-pwhoolboom.name
  description = "ECR repository name"
}
