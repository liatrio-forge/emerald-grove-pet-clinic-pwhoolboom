# 18-tasks-terraform-infrastructure-modules.md

## Relevant Files

- `infra/.gitignore` - Terraform-specific ignores (`.terraform/`, `*.tfstate`, `*.tfvars`, `.terraform.lock.hcl`)
- `infra/modules/ecr/main.tf` - ECR repository resource and lifecycle policy
- `infra/modules/ecr/variables.tf` - Input variables for the ECR module
- `infra/modules/ecr/outputs.tf` - Output: `repository_url`, `repository_name`
- `infra/modules/rds/rds.tf` - Random password, DB subnet group, and RDS instance resources
- `infra/modules/rds/security.tf` - RDS security group allowing port 5432 from ECS tasks SG
- `infra/modules/rds/ssm.tf` - SSM parameters for db/url, db/username, db/password
- `infra/modules/rds/remote_state.tf` - Platform remote state data source; locals for `data_subnet_ids` and `vpc_id`
- `infra/modules/rds/variables.tf` - Input variables for the RDS module
- `infra/modules/rds/outputs.tf` - Outputs: `rds_endpoint`, `rds_security_group_id`, SSM parameter ARNs
- `infra/modules/fargate/main.tf` - CloudWatch log group, ECS task definition, and ECS service resources
- `infra/modules/fargate/iam.tf` - IAM execution role and task role with permissions boundary
- `infra/modules/fargate/alb.tf` - ALB, target group, and HTTP listener resources
- `infra/modules/fargate/security.tf` - ALB security group and ECS tasks security group
- `infra/modules/fargate/ssm.tf` - Anthropic API key SSM placeholder parameter
- `infra/modules/fargate/remote_state.tf` - Platform remote state data source; locals for VPC, subnets, ECS cluster
- `infra/modules/fargate/variables.tf` - Input variables for the Fargate module
- `infra/modules/fargate/outputs.tf` - Outputs: ALB DNS, ECS service name, security group IDs, etc.
- `infra/envs/dev/main.tf` - Composes all three modules with dev-specific config and cross-module wiring
- `infra/envs/dev/variables.tf` - Input variables for the dev environment
- `infra/envs/dev/providers.tf` - Terraform version constraints and AWS/Random provider declarations
- `infra/envs/dev/backend.tf` - S3 backend configuration for remote state
- `infra/envs/dev/outputs.tf` - Environment-level outputs (ALB DNS, ECR URL, RDS endpoint, etc.)
- `docs/specs/18-spec-terraform-infrastructure-modules/proof/terraform-plan-output.txt` - Saved `terraform show tfplan` output (redacted) as proof of a clean plan

### Notes

- There are no automated tests for Terraform modules in the traditional sense. Validation is done via `terraform validate` (syntax/schema check) and `terraform plan` (full graph resolution against real AWS state).
- Run `terraform init -backend=false` before `terraform validate` when validating individual modules in isolation — this initializes providers without requiring an S3 backend.
- All resources must be tagged with `Name`, `Project` (`var.project_name`), and `Environment` (`var.environment`) to match the reference implementation's conventions.
- The circular dependency between the RDS module (needs ECS SG ID) and the Fargate module (needs SSM ARNs for secrets injection) is broken by having the Fargate module **construct SSM ARNs internally** from known variables (`aws_region`, `aws_account_id`, `environment`) rather than accepting them as inputs. This means: Fargate → no dependency on RDS; RDS → depends on Fargate (for ECS SG ID output).
- Do **not** commit `terraform.tfstate`, `*.tfvars` files with real account IDs or credentials, or `.terraform/` directories. These are excluded by `infra/.gitignore`.
- The proof artifact in `proof/terraform-plan-output.txt` must have all sensitive values and real account IDs redacted before committing.

## Tasks

### [x] 1.0 Create ECR Module

#### 1.0 Proof Artifact(s)

- CLI: `terraform validate` run from `infra/modules/ecr/` exits with code 0 and prints "Success! The configuration is valid." demonstrates the module is syntactically correct
- CLI: `terraform plan` output from `envs/dev/` shows `aws_ecr_repository.pet-clinic-pwhoolboom` and `aws_ecr_lifecycle_policy.pet-clinic-pwhoolboom` as resources to be created, demonstrating the ECR module is correctly wired into the environment

#### 1.0 Tasks

- [x] 1.1 Create `infra/.gitignore` containing the following entries to prevent Terraform state and secrets from being committed:

  ```text
  .terraform/
  .terraform.lock.hcl
  *.tfstate
  *.tfstate.backup
  *.tfvars
  tfplan
  ```

- [x] 1.2 Create `infra/modules/ecr/variables.tf` declaring three input variables:
  - `project_name` — type `string`, no default (required), description `"Team/project name used in resource naming"`
  - `environment` — type `string`, default `"dev"`, description `"Deployment environment (e.g., dev, prod)"`
  - `aws_region` — type `string`, default `"us-east-1"`, description `"AWS region"`

- [x] 1.3 Create `infra/modules/ecr/main.tf` with an `aws_ecr_repository` resource with the logical name `pet-clinic-pwhoolboom` (the Terraform resource label, not the repo name). Set:
  - `name = "pet-clinic-pwhoolboom"` (hardcoded, matching the spec)
  - `image_tag_mutability = "MUTABLE"`
  - `image_scanning_configuration { scan_on_push = true }`
  - Tags: `Name = "pet-clinic-pwhoolboom"`, `Project = var.project_name`, `Environment = var.environment`

- [x] 1.4 Add an `aws_ecr_lifecycle_policy` resource to `infra/modules/ecr/main.tf` attached to `aws_ecr_repository.pet-clinic-pwhoolboom.name`. The `policy` must be a JSON string with two rules:
  - **Rule 1** (priority 1): `selection.tagStatus = "untagged"`, `action.type = "expire"`, `description = "Expire untagged images after 7 days"`, countType `"sinceImagePushed"`, countNumber `7`, countUnit `"days"`
  - **Rule 2** (priority 2): `selection.tagStatus = "tagged"`, `selection.tagPrefixList = ["v"]`, `action.type = "expire"`, `description = "Keep only 10 most recent versioned images"`, countType `"imageCountMoreThan"`, countNumber `10`

- [x] 1.5 Create `infra/modules/ecr/outputs.tf` declaring two outputs:
  - `repository_url` — value: `aws_ecr_repository.pet-clinic-pwhoolboom.repository_url`, description `"ECR repository URL"`
  - `repository_name` — value: `aws_ecr_repository.pet-clinic-pwhoolboom.name`, description `"ECR repository name"`

- [x] 1.6 Run `terraform init -backend=false` then `terraform validate` in `infra/modules/ecr/`. The output must be `"Success! The configuration is valid."` Fix any reported errors before moving on.

---

### [x] 2.0 Create RDS Module

#### 2.0 Proof Artifact(s)

- CLI: `terraform validate` run from `infra/modules/rds/` exits with code 0 and prints "Success! The configuration is valid." demonstrates the module is syntactically correct
- CLI: `terraform plan` output from `envs/dev/` shows `aws_db_instance`, `aws_db_subnet_group`, `aws_security_group` (RDS), `random_password`, and three `aws_ssm_parameter` resources (`db/url`, `db/username`, `db/password`) as resources to be created, demonstrating the RDS module is correctly wired

#### 2.0 Tasks

- [x] 2.1 Create the directory `infra/modules/rds/`.

- [x] 2.2 Create `infra/modules/rds/variables.tf` declaring the following input variables:
  - `project_name` — type `string`, required
  - `environment` — type `string`, default `"dev"`
  - `aws_region` — type `string`, default `"us-east-1"`
  - `aws_account_id` — type `number`, required, description `"AWS account ID"`
  - `ecs_security_group_id` — type `string`, required, description `"Security group ID of the ECS tasks; used to allow inbound PostgreSQL access to the RDS instance"`
  - `db_name` — type `string`, default `"petclinic"`, description `"PostgreSQL database name"`
  - `db_username` — type `string`, default `"petclinic"`, description `"PostgreSQL master username"`
  - `platform_state_bucket` — type `string`, default `"terraform-state-ecs-landingzone-dev"`, description `"S3 bucket containing the platform Terraform state"`
  - `platform_state_key` — type `string`, default `"ecs-application-landingzone/terraform.tfstate"`, description `"S3 key for the platform Terraform state file"`
  - `platform_state_region` — type `string`, default `"us-east-1"`, description `"AWS region of the platform state S3 bucket"`

- [x] 2.3 Create `infra/modules/rds/remote_state.tf` with a `data "terraform_remote_state" "platform"` block that reads from the S3 backend using `var.platform_state_bucket`, `var.platform_state_key`, and `var.platform_state_region`. Add a `locals` block extracting:
  - `data_subnet_ids = data.terraform_remote_state.platform.outputs.data_subnet_ids`
  - `vpc_id         = data.terraform_remote_state.platform.outputs.vpc_id`

- [x] 2.4 Create `infra/modules/rds/security.tf` with an `aws_security_group` resource (logical name `rds`) named `${var.project_name}-rds-${var.environment}` in `local.vpc_id`. Add:
  - One ingress rule: protocol `"tcp"`, from_port `5432`, to_port `5432`, `source_security_group_id = var.ecs_security_group_id`, description `"Allow PostgreSQL access from ECS tasks"`
  - One egress rule: protocol `"-1"`, from_port `0`, to_port `0`, cidr_blocks `["0.0.0.0/0"]`
  - Tags: `Name`, `Project`, `Environment`

- [x] 2.5 Create `infra/modules/rds/rds.tf` with three resources:
  1. `resource "random_password" "db_password"`: `length = 16`, `special = true`
  2. `resource "aws_db_subnet_group" "main"`: `name = "${var.project_name}-${var.environment}"`, `subnet_ids = local.data_subnet_ids`. Tag with Name, Project, Environment.
  3. `resource "aws_db_instance" "main"`: `identifier = "${var.project_name}-db-${var.environment}"`, `engine = "postgres"`, `engine_version = "15.12"`, `instance_class = "db.t3.micro"`, `allocated_storage = 20`, `storage_type = "gp3"`, `storage_encrypted = true`, `db_name = var.db_name`, `username = var.db_username`, `password = random_password.db_password.result`, `db_subnet_group_name = aws_db_subnet_group.main.name`, `vpc_security_group_ids = [aws_security_group.rds.id]`, `publicly_accessible = false`, `skip_final_snapshot = true`. Tag with Name, Project, Environment.

- [x] 2.6 Create `infra/modules/rds/ssm.tf` with three `aws_ssm_parameter` resources, all of type `"SecureString"`:
  1. Logical name `db_url`, name `/pet-clinic-pwhoolboom/${var.environment}/db/url`, value `"jdbc:postgresql://${aws_db_instance.main.endpoint}/${var.db_name}"`
  2. Logical name `db_username`, name `/pet-clinic-pwhoolboom/${var.environment}/db/username`, value `var.db_username`
  3. Logical name `db_password`, name `/pet-clinic-pwhoolboom/${var.environment}/db/password`, value `random_password.db_password.result` — mark this output as `sensitive = true` in the resource. Tag all three with Name, Project, Environment.

- [x] 2.7 Create `infra/modules/rds/outputs.tf` declaring:
  - `rds_endpoint` — value: `aws_db_instance.main.endpoint`
  - `rds_security_group_id` — value: `aws_security_group.rds.id`
  - `ssm_db_url_arn` — value: `aws_ssm_parameter.db_url.arn`
  - `ssm_db_username_arn` — value: `aws_ssm_parameter.db_username.arn`
  - `ssm_db_password_arn` — value: `aws_ssm_parameter.db_password.arn`

- [x] 2.8 Run `terraform init -backend=false` then `terraform validate` in `infra/modules/rds/`. The output must be `"Success! The configuration is valid."` Fix any errors before moving on.

---

### [x] 3.0 Create Fargate Module (with ALB and IAM)

#### 3.0 Proof Artifact(s)

- CLI: `terraform validate` run from `infra/modules/fargate/` exits with code 0 and prints "Success! The configuration is valid." demonstrates the module is syntactically correct
- CLI: `terraform plan` output from `envs/dev/` shows `aws_ecs_task_definition`, `aws_ecs_service`, `aws_lb`, `aws_lb_target_group`, `aws_lb_listener`, `aws_iam_role.execution`, `aws_iam_role.task`, `aws_cloudwatch_log_group`, two `aws_security_group` resources (ALB and ECS tasks), and `aws_ssm_parameter` (anthropic/api-key) as resources to be created, demonstrating the Fargate module is correctly wired

#### 3.0 Tasks

- [x] 3.1 Create the directory `infra/modules/fargate/`.

- [x] 3.2 Create `infra/modules/fargate/variables.tf` declaring:
  - `project_name` — type `string`, required
  - `environment` — type `string`, default `"dev"`
  - `aws_region` — type `string`, default `"us-east-1"`
  - `aws_account_id` — type `number`, required
  - `container_port` — type `number`, default `80`, description `"Port the container listens on"`
  - `cpu` — type `number`, default `256`, description `"ECS task CPU units (256, 512, 1024, 2048, 4096)"`
  - `memory` — type `number`, default `512`, description `"ECS task memory in MB"`
  - `desired_count` — type `number`, default `2`, description `"Desired number of running ECS tasks"`
  - `health_check_path` — type `string`, default `"/"`, description `"ALB health check path"`
  - `ecr_repository_url` — type `string`, required, description `"Full URL of the ECR repository to pull the container image from (without tag)"`
  - `environment_variables` — type `list(object({ name = string, value = string }))`, default `[]`, description `"Environment variables to inject into the container"`
  - `platform_state_bucket` — type `string`, default `"terraform-state-ecs-landingzone-dev"`
  - `platform_state_key` — type `string`, default `"ecs-application-landingzone/terraform.tfstate"`
  - `platform_state_region` — type `string`, default `"us-east-1"`

- [x] 3.3 Create `infra/modules/fargate/remote_state.tf` with a `data "terraform_remote_state" "platform"` block reading the S3 backend. Add a `locals` block extracting:
  - `vpc_id             = data.terraform_remote_state.platform.outputs.vpc_id`
  - `private_subnet_ids = data.terraform_remote_state.platform.outputs.private_subnet_ids`
  - `public_subnet_ids  = data.terraform_remote_state.platform.outputs.public_subnet_ids`
  - `ecs_cluster_name   = data.terraform_remote_state.platform.outputs.ecs_cluster_name`

- [x] 3.4 Create `infra/modules/fargate/security.tf` with two security groups:
  1. **ALB SG** (logical name `alb`), name `${var.project_name}-alb-${var.environment}`, in `local.vpc_id`: ingress on port 80 from `0.0.0.0/0`; ingress on port 443 from `0.0.0.0/0`; egress all traffic.
  2. **ECS tasks SG** (logical name `ecs_tasks`), name `${var.project_name}-ecs-tasks-${var.environment}`, in `local.vpc_id`: ingress all TCP (from_port `0`, to_port `65535`) from `aws_security_group.alb.id`; egress all traffic.
  Tag both with Name, Project, Environment.

- [x] 3.5 Create `infra/modules/fargate/ssm.tf` with a single `aws_ssm_parameter` resource (logical name `anthropic_api_key`):
  - `name  = "/pet-clinic-pwhoolboom/${var.environment}/anthropic/api-key"`
  - `type  = "SecureString"`
  - `value = "PLACEHOLDER"` (initial value only; will be replaced manually in AWS)
  - Add `lifecycle { ignore_changes = [value] }` so Terraform never overwrites a manually-set key value
  - Tag with Name, Project, Environment

- [x] 3.6 Create `infra/modules/fargate/iam.tf` with two IAM roles. Both roles must include `permissions_boundary = "arn:aws:iam::${var.aws_account_id}:policy/team-permissions-boundary-v1"`.

  **Execution role** (logical name `execution`):
  - `name = "${var.project_name}-pet-clinic-execution-${var.environment}"`
  - Assume role policy: `Service = "ecs-tasks.amazonaws.com"`, `Action = "sts:AssumeRole"`
  - Attach managed policy `arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy` via `aws_iam_role_policy_attachment`
  - Add an inline `aws_iam_role_policy` named `ssm-access` granting `ssm:GetParameters` and `ssm:GetParameter` on resource `arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/pet-clinic-pwhoolboom/${var.environment}/*`

  **Task role** (logical name `task`):
  - `name = "${var.project_name}-pet-clinic-task-${var.environment}"`
  - Assume role policy: `Service = "ecs-tasks.amazonaws.com"`, `Action = "sts:AssumeRole"`
  - Add an inline `aws_iam_role_policy` named `cloudwatch-logs` granting `logs:CreateLogStream` and `logs:PutLogEvents` on resource `${aws_cloudwatch_log_group.main.arn}:*` (this references the log group defined in `main.tf`; Terraform resolves cross-file references within the same module automatically)

- [x] 3.7 Create `infra/modules/fargate/alb.tf` with three resources:
  1. `aws_lb` (logical name `main`): `name = "${var.project_name}-alb-${var.environment}"`, `internal = false`, `load_balancer_type = "application"`, `security_groups = [aws_security_group.alb.id]`, `subnets = local.public_subnet_ids`. Tag with Name, Project, Environment.
  2. `aws_lb_target_group` (logical name `main`): `name = "${var.project_name}-${var.environment}"` (ensure this is ≤32 characters), `target_type = "ip"`, `port = var.container_port`, `protocol = "HTTP"`, `vpc_id = local.vpc_id`. Health check block: `path = var.health_check_path`, `matcher = "200-399"`, `interval = 30`. Tag with Name, Project, Environment.
  3. `aws_lb_listener` (logical name `http`): `load_balancer_arn = aws_lb.main.arn`, `port = 80`, `protocol = "HTTP"`. Default action: `type = "forward"`, `target_group_arn = aws_lb_target_group.main.arn`.

- [x] 3.8 Create `infra/modules/fargate/main.tf` with three resources:
  1. `aws_cloudwatch_log_group` (logical name `main`): `name = "/ecs/${var.project_name}-pet-clinic/${var.environment}"`, `retention_in_days = 30`. Tag with Name, Project, Environment.
  2. `aws_ecs_task_definition` (logical name `main`): `family = "${var.project_name}-pet-clinic-${var.environment}"`, `network_mode = "awsvpc"`, `requires_compatibilities = ["FARGATE"]`, `cpu = var.cpu`, `memory = var.memory`, `execution_role_arn = aws_iam_role.execution.arn`, `task_role_arn = aws_iam_role.task.arn`. The `container_definitions` JSON must include one container named `"pet-clinic"` with:
     - `image`: `"${var.ecr_repository_url}:latest"`
     - `portMappings`: `[{ "containerPort": var.container_port, "protocol": "tcp" }]`
     - `environment`: use `var.environment_variables` (passed through as-is)
     - `logConfiguration`: driver `"awslogs"`, options `awslogs-group`, `awslogs-region`, `awslogs-stream-prefix = "ecs"`
     - `secrets`: four entries, each with a `name` and `valueFrom` (the SSM parameter ARN constructed from known variables):
       - `SPRING_DATASOURCE_URL` → `"arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/pet-clinic-pwhoolboom/${var.environment}/db/url"`
       - `USERNAME` → `"arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/pet-clinic-pwhoolboom/${var.environment}/db/username"`
       - `PASSWORD` → `"arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/pet-clinic-pwhoolboom/${var.environment}/db/password"`
       - `ANTHROPIC_API_KEY` → `"arn:aws:ssm:${var.aws_region}:${var.aws_account_id}:parameter/pet-clinic-pwhoolboom/${var.environment}/anthropic/api-key"`

     Use `jsonencode()` to build the container definitions rather than an inline JSON string — it is safer and easier to read.

  3. `aws_ecs_service` (logical name `main`): `name = "${var.project_name}-pet-clinic-${var.environment}"`, `cluster = local.ecs_cluster_name`, `task_definition = aws_ecs_task_definition.main.arn`, `desired_count = var.desired_count`, `launch_type = "FARGATE"`. Network configuration: `subnets = local.private_subnet_ids`, `security_groups = [aws_security_group.ecs_tasks.id]`, `assign_public_ip = false`. Load balancer block: `target_group_arn = aws_lb_target_group.main.arn`, `container_name = "pet-clinic"`, `container_port = var.container_port`. Add `depends_on = [aws_lb_listener.http]` to ensure the listener exists before the service is created.

- [x] 3.9 Create `infra/modules/fargate/outputs.tf` declaring:
  - `alb_dns_name` — value: `aws_lb.main.dns_name`
  - `alb_zone_id` — value: `aws_lb.main.zone_id`
  - `alb_arn` — value: `aws_lb.main.arn`
  - `ecs_service_name` — value: `aws_ecs_service.main.name`
  - `ecs_task_definition_arn` — value: `aws_ecs_task_definition.main.arn`
  - `cloudwatch_log_group` — value: `aws_cloudwatch_log_group.main.name`
  - `ecs_tasks_security_group_id` — value: `aws_security_group.ecs_tasks.id`
  - `alb_security_group_id` — value: `aws_security_group.alb.id`
  - `target_group_arn` — value: `aws_lb_target_group.main.arn`

- [x] 3.10 Run `terraform init -backend=false` then `terraform validate` in `infra/modules/fargate/`. The output must be `"Success! The configuration is valid."` Fix any errors before moving on.

---

### [ ] 4.0 Compose Dev Environment and Validate Full Plan

#### 4.0 Proof Artifact(s)

- CLI: `terraform plan -out=tfplan` run from `infra/envs/dev/` exits with code 0 with no errors or warnings, demonstrating all three modules compose correctly and all cross-module references resolve (ECR URL → Fargate task definition; ECS tasks security group ID → RDS security group ingress rule)
- CLI: `terraform show tfplan` output saved (with sensitive values and account IDs redacted) to `docs/specs/18-spec-terraform-infrastructure-modules/proof/terraform-plan-output.txt` showing the complete resource list across all three modules, demonstrating correct dependency resolution and end-to-end wiring

#### 4.0 Tasks

- [ ] 4.1 Create the directory `infra/envs/dev/`.

- [ ] 4.2 Create `infra/envs/dev/providers.tf`:

  ```hcl
  terraform {
    required_version = ">= 1.0"
    required_providers {
      aws = {
        source  = "hashicorp/aws"
        version = "~> 5.0"
      }
      random = {
        source  = "hashicorp/random"
        version = "~> 3.0"
      }
    }
  }

  provider "aws" {
    region = var.aws_region
  }
  ```

- [ ] 4.3 Create `infra/envs/dev/backend.tf`:

  ```hcl
  terraform {
    backend "s3" {
      bucket         = "terraform-state-ecs-landingzone-dev"
      key            = "apps/pet-clinic-pwhoolboom/terraform.tfstate"
      region         = "us-east-1"
      dynamodb_table = "terraform-state-lock"
      encrypt        = true
    }
  }
  ```

- [ ] 4.4 Create `infra/envs/dev/variables.tf` declaring:
  - `aws_region` — type `string`, default `"us-east-1"`
  - `project_name` — type `string`, required, description `"Team/project name used in resource naming"`
  - `aws_account_id` — type `number`, required, description `"AWS account ID"`

- [ ] 4.5 Create `infra/envs/dev/main.tf` calling all three modules. The dependency order is: **ECR** and **Fargate** have no dependency on each other; **RDS** depends on the Fargate module output `ecs_tasks_security_group_id`. Use the following module blocks:

  ```hcl
  module "ecr" {
    source       = "../../modules/ecr"
    project_name = var.project_name
    environment  = "dev"
    aws_region   = var.aws_region
  }

  module "fargate" {
    source             = "../../modules/fargate"
    project_name       = var.project_name
    environment        = "dev"
    aws_region         = var.aws_region
    aws_account_id     = var.aws_account_id
    container_port     = 8080
    cpu                = 512
    memory             = 1024
    health_check_path  = "/actuator/health"
    ecr_repository_url = module.ecr.repository_url
    environment_variables = [
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "postgres,aws"
      }
    ]
  }

  module "rds" {
    source                = "../../modules/rds"
    project_name          = var.project_name
    environment           = "dev"
    aws_region            = var.aws_region
    aws_account_id        = var.aws_account_id
    ecs_security_group_id = module.fargate.ecs_tasks_security_group_id
  }
  ```

- [ ] 4.6 Create `infra/envs/dev/outputs.tf` declaring:
  - `ecr_repository_url` — value: `module.ecr.repository_url`, description `"ECR repository URL for pushing images"`
  - `alb_dns_name` — value: `module.fargate.alb_dns_name`, description `"ALB DNS name — use this to access the application over HTTP"`
  - `ecs_service_name` — value: `module.fargate.ecs_service_name`
  - `rds_endpoint` — value: `module.rds.rds_endpoint`
  - `cloudwatch_log_group` — value: `module.fargate.cloudwatch_log_group`

- [ ] 4.7 Run `terraform init -backend=false` in `infra/envs/dev/`. This downloads the AWS and Random provider plugins and links the local modules without connecting to the S3 backend. Confirm the command completes without errors.

- [ ] 4.8 Run `terraform validate` in `infra/envs/dev/`. The output must be `"Success! The configuration is valid."` If there are errors, trace them back to the relevant module file and fix them before proceeding.

- [ ] 4.9 Create the proof artifact directory `docs/specs/18-spec-terraform-infrastructure-modules/proof/`. Run `terraform plan -out=tfplan` from `infra/envs/dev/` (requires real AWS credentials and S3 backend access; pass required variables via `-var="project_name=pwhoolboom" -var="aws_account_id=<your-account-id>"`). If the backend is inaccessible, run with `-backend-config` overrides or use `terraform plan -backend=false` to validate resource graph resolution. Save the output of `terraform show tfplan` to `docs/specs/18-spec-terraform-infrastructure-modules/proof/terraform-plan-output.txt`. Before committing: replace the real AWS account ID with `<ACCOUNT_ID>` and replace any sensitive values with `<REDACTED>`.
