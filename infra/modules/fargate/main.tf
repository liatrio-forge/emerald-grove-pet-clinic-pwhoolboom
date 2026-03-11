resource "aws_cloudwatch_log_group" "main" {
  name              = "/ecs/${var.project_name}-pet-clinic/${var.environment}"
  retention_in_days = 30

  tags = {
    Name        = "/ecs/${var.project_name}-pet-clinic/${var.environment}"
    Project     = var.project_name
    Environment = var.environment
  }
}

resource "aws_ecs_task_definition" "main" {
  family                   = "${var.project_name}-pet-clinic-${var.environment}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.cpu
  memory                   = var.memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name  = "pet-clinic"
      image = "${var.ecr_repository_url}:latest"
      portMappings = [
        {
          containerPort = var.container_port
          protocol      = "tcp"
        }
      ]
      environment = var.environment_variables
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.main.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
      secrets = [
        {
          name      = "SPRING_DATASOURCE_URL"
          valueFrom = "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/pet-clinic-pwhoolboom/${var.environment}/db/url"
        },
        {
          name      = "USERNAME"
          valueFrom = "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/pet-clinic-pwhoolboom/${var.environment}/db/username"
        },
        {
          name      = "PASSWORD"
          valueFrom = "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/pet-clinic-pwhoolboom/${var.environment}/db/password"
        },
        {
          name      = "ANTHROPIC_API_KEY"
          valueFrom = "arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/pet-clinic-pwhoolboom/${var.environment}/anthropic/api-key"
        }
      ]
    }
  ])
}

resource "aws_ecs_service" "main" {
  name            = "${var.project_name}-pet-clinic-${var.environment}"
  cluster         = local.ecs_cluster_name
  task_definition = aws_ecs_task_definition.main.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = local.private_subnet_ids
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.main.arn
    container_name   = "pet-clinic"
    container_port   = var.container_port
  }

  depends_on = [aws_lb_listener.http]
}
