resource "aws_security_group" "rds" {
  name   = "${var.project_name}-rds-${var.environment}"
  vpc_id = local.vpc_id

  ingress {
    protocol                 = "tcp"
    from_port                = 5432
    to_port                  = 5432
    security_groups          = [var.ecs_security_group_id]
    description              = "Allow PostgreSQL access from ECS tasks"
  }

  egress {
    protocol    = "-1"
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.project_name}-rds-${var.environment}"
    Project     = var.project_name
    Environment = var.environment
  }
}
