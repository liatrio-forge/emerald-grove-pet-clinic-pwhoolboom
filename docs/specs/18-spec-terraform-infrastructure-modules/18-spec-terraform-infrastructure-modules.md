# 18-spec-terraform-infrastructure-modules.md

## Introduction/Overview

This feature delivers a suite of Terraform modules that provision the AWS infrastructure required to run the Emerald Grove Pet Clinic application under the `pwhoolboom` project. The modules cover container image storage (ECR), serverless compute (Fargate/ECS with ALB), and a managed PostgreSQL database (RDS), along with all required IAM roles and policies. The goal is to replace the `toddwells`-specific reference implementation with a clean, `pwhoolboom`-branded equivalent organized as three composable modules.

## Goals

- Create three separate, focused Terraform modules: `ecr`, `fargate`, and `rds`
- Replace all references to `toddwells` / `twells` with `pwhoolboom` in resource names, SSM paths, and ECR repository names
- Include all necessary IAM roles and policies with the `team-permissions-boundary-v1` permissions boundary enforced
- Provide a `dev` environment under `envs/dev/` that composes the three modules
- Maintain parity with the reference implementation's security posture, secret management, and platform remote state integration

## User Stories

- **As a developer**, I want to push container images to an ECR repository named `pet-clinic-pwhoolboom` so that I can deploy the application to Fargate.
- **As a developer**, I want to run `terraform apply` in `envs/dev/` and have a working Fargate service with an ALB so that I can access the pet clinic application over HTTP.
- **As a developer**, I want a managed RDS PostgreSQL instance provisioned with secrets stored in SSM Parameter Store so that the application can connect to the database securely without hardcoded credentials.
- **As a platform engineer**, I want all IAM roles to have the `team-permissions-boundary-v1` permissions boundary applied so that the deployment stays within organizational governance controls.

## Demoable Units of Work

### Unit 1: ECR Module

**Purpose:** Provisions the ECR container registry for the pet clinic image, including lifecycle policies that keep the registry tidy.

**Functional Requirements:**

- The system shall create an ECR repository named `pet-clinic-pwhoolboom`
- The system shall enable image scanning on push
- The system shall apply a lifecycle policy that expires untagged images after 7 days
- The system shall apply a lifecycle policy that retains only the 10 most recent images with a `v` tag prefix
- The module shall expose the repository URL as an output

**Proof Artifacts:**

- `terraform plan` output: Shows `aws_ecr_repository.pet-clinic-pwhoolboom` and `aws_ecr_lifecycle_policy` in the plan, demonstrating the module is correctly defined
- `terraform apply` output / AWS Console screenshot: ECR repository `pet-clinic-pwhoolboom` exists with scan-on-push enabled and lifecycle rules configured, demonstrating successful provisioning

---

### Unit 2: RDS Module

**Purpose:** Provisions a managed PostgreSQL 15 database instance with auto-generated credentials stored in SSM Parameter Store, accessible only from within the VPC.

**Functional Requirements:**

- The system shall create an RDS PostgreSQL 15 instance with identifier `${project_name}-db-${environment}`
- The system shall use instance class `db.t3.micro` with 20GB `gp3` encrypted storage
- The system shall generate a random 16-character database password and store it in SSM Parameter Store at `/pet-clinic-pwhoolboom/${environment}/db/password`
- The system shall store the JDBC connection URL at `/pet-clinic-pwhoolboom/${environment}/db/url` and the username at `/pet-clinic-pwhoolboom/${environment}/db/username`
- The system shall place the RDS instance in the data subnets read from platform remote state
- The system shall create a security group that allows port 5432 only from a provided ECS tasks security group ID
- The system shall set `publicly_accessible = false` and skip the final snapshot
- The module shall expose the RDS endpoint and the SSM parameter paths as outputs

**Proof Artifacts:**

- `terraform plan` output: Shows `aws_db_instance`, `aws_db_subnet_group`, `aws_security_group` (RDS), `aws_ssm_parameter` (db/url, db/username, db/password), and `random_password` resources, demonstrating the module is correctly defined
- `terraform apply` output / AWS Console screenshot: RDS instance exists in `available` state, SSM parameters visible (values redacted), demonstrating successful provisioning

---

### Unit 3: Fargate Module (with ALB and IAM)

**Purpose:** Provisions the ECS task definition, Fargate service, Application Load Balancer, IAM execution and task roles, and injects database secrets from SSM into the running container.

**Functional Requirements:**

- The system shall create an ECS task definition with family `${project_name}-pet-clinic-${environment}` using Fargate launch type
- The system shall inject SSM secrets (SPRING_DATASOURCE_URL, USERNAME, PASSWORD, ANTHROPIC_API_KEY) into the container via `secrets` in the task definition
- The system shall create an SSM parameter at `/pet-clinic-pwhoolboom/${environment}/anthropic/api-key` as a placeholder with `lifecycle.ignore_changes` so manual values are not overwritten
- The system shall create an IAM execution role named `${project_name}-pet-clinic-execution-${environment}` with the `AmazonECSTaskExecutionRolePolicy` managed policy attached and an inline policy granting `ssm:GetParameters` and `ssm:GetParameter` on `/pet-clinic-pwhoolboom/${environment}/*`
- The system shall create an IAM task role named `${project_name}-pet-clinic-task-${environment}` with an inline policy granting CloudWatch log write permissions
- Both IAM roles shall have `arn:aws:iam::${account_id}:policy/team-permissions-boundary-v1` set as the permissions boundary
- The system shall create an Application Load Balancer named `${project_name}-alb-${environment}` in public subnets with an HTTP listener on port 80 forwarding to the ECS service
- The system shall create security groups such that: ALB accepts HTTP/HTTPS from `0.0.0.0/0`; ECS tasks accept traffic only from the ALB security group; RDS security group (passed as an input) accepts 5432 only from ECS tasks
- The system shall create a CloudWatch log group at `/ecs/${project_name}-pet-clinic/${environment}`
- The module shall expose the ALB DNS name, ECR repository URL (passed in), ECS service name, and task definition ARN as outputs

**Proof Artifacts:**

- `terraform plan` output: Shows all expected resources (ECS task def, ECS service, ALB, target group, listener, two IAM roles, CloudWatch log group, security groups, SSM Anthropic parameter), demonstrating the module is correctly defined
- `terraform apply` output: All resources created with no errors, demonstrating successful provisioning
- AWS Console screenshot or `aws ecs describe-services` CLI output: ECS service shows `ACTIVE` status with desired count running, demonstrating the service is operational
- Browser screenshot or `curl http://<alb-dns-name>/actuator/health`: Returns HTTP 200, demonstrating the application is reachable through the ALB

---

### Unit 4: Dev Environment Composition

**Purpose:** Wires the three modules together into a deployable `envs/dev/` environment configuration with an S3 backend for state, demonstrating the modules work together end-to-end.

**Functional Requirements:**

- The system shall compose `ecr`, `rds`, and `fargate` modules in `envs/dev/main.tf`
- The `dev` environment shall configure the container port as `8080`, health check path as `/actuator/health`, CPU as `512`, memory as `1024`, and `SPRING_PROFILES_ACTIVE=postgres,aws`
- The environment shall pass the ECS tasks security group ID from the fargate module into the rds module (or pass the rds security group into fargate), ensuring the modules are correctly wired
- The system shall use an S3 backend with bucket `terraform-state-ecs-landingzone-dev`, key `apps/pet-clinic-pwhoolboom/terraform.tfstate`, and DynamoDB table `terraform-state-lock`
- The system shall read platform state (VPC ID, subnet IDs, ECS cluster name) from configurable remote state variables whose defaults match the reference implementation

**Proof Artifacts:**

- `terraform plan -out=tfplan` from `envs/dev/`: Exit code 0 with a complete plan covering all three modules' resources, demonstrating correct module composition and variable wiring
- `terraform show tfplan` output excerpt: Highlights the cross-module wiring (e.g., ECS task def referencing the ECR URL output, RDS security group referencing the ECS tasks security group), demonstrating correct dependency resolution

## Non-Goals (Out of Scope)

1. **Production environment**: No `envs/prod/` directory will be created; only `dev` is in scope.
2. **DNS / Route53**: No custom domain name or Route53 records are included; the ALB DNS name is the access point.
3. **HTTPS / ACM certificates**: The ALB will only have an HTTP listener on port 80; TLS termination is out of scope.
4. **CI/CD pipeline changes**: This spec covers only Terraform infrastructure modules, not GitHub Actions workflows or ECR push automation.
5. **Application code changes**: No modifications to the Spring Boot application or its configuration files.
6. **Migrating existing toddwells resources**: This is a net-new deployment; no import or migration of existing `toddwells` infrastructure.

## Design Considerations

No specific UI/UX design requirements. The ALB DNS name is the public access point for the application. The health check path `/actuator/health` must return HTTP 200-399 for the ECS service to reach a healthy state.

## Repository Standards

The reference implementation (`emerald-grove-pet-clinic-toddwells/infra/`) defines the following patterns that this implementation shall follow:

- **File organization**: Each module contains `main.tf`, `variables.tf`, `outputs.tf`, `iam.tf` (where applicable). Separate `.tf` files per resource concern (e.g., `ecr.tf`, `rds.tf`, `alb.tf`, `ssm.tf`, `security.tf`, `remote_state.tf`)
- **Resource naming**: `${var.project_name}-<resource-type>-${var.environment}` (e.g., `pwhoolboom-alb-dev`)
- **Tagging**: All resources tagged with `Name`, `Project`, and `Environment`
- **Provider versions**: Terraform `>= 1.0`, AWS provider `~> 5.0`, Random provider `~> 3.0`
- **Secrets**: Use SSM Parameter Store `SecureString` type; never store plaintext passwords in Terraform state beyond what is unavoidable with `random_password`
- **Platform state**: Use a `data "terraform_remote_state"` block to read shared VPC/subnet/ECS cluster outputs
- **Permissions boundary**: All IAM roles must include `permissions_boundary = "arn:aws:iam::${var.aws_account_id}:policy/team-permissions-boundary-v1"`

## Technical Considerations

- **Module separation**: Because ECR is a global/shared resource (not per-environment), it may be invoked independently of the fargate and rds modules. The `fargate` module accepts the ECR repository URL as an input variable.
- **Cross-module dependency**: The `rds` module needs the ECS tasks security group ID to create its ingress rule. This can be wired at the `envs/dev/` level: fargate module output → rds module input, or vice versa. The preferred approach (matching the reference) is to have the rds module accept an `ecs_security_group_id` variable.
- **Random password**: The `random_password` resource lives in the `rds` module. The generated value is written to SSM; it is sensitive in Terraform state.
- **ECR URL reference**: The `fargate` module's ECS task definition references the ECR repository URL. This URL must be passed from the `ecr` module output into the `fargate` module as an input variable.
- **Platform remote state**: The `remote_state.tf` pattern from the reference must be replicated in both the `rds` and `fargate` modules (or extracted into the env-level `main.tf` and passed down as variables). Preference: each module that needs VPC/subnet data reads it directly, keeping modules self-contained.
- **Terraform providers**: The `random` provider is required in any module using `random_password`. Ensure it is declared in `envs/dev/providers.tf`.

## Security Considerations

- **Database password**: Auto-generated by `random_password`; stored as SSM `SecureString`. Never hardcoded.
- **SSM parameter paths**: All paths use the `/pet-clinic-pwhoolboom/` prefix. IAM execution role policy must reference this exact prefix.
- **Permissions boundary**: `team-permissions-boundary-v1` must be applied to all IAM roles to satisfy organizational governance.
- **Anthropic API key**: The SSM parameter is created as a placeholder. The actual key value must be set manually in AWS and is protected by `lifecycle { ignore_changes = [value] }` to prevent Terraform from overwriting it.
- **Proof artifacts**: Do not commit `terraform.tfstate`, `*.tfvars` files containing secrets, or AWS Console screenshots that expose account IDs or unredacted credentials to the repository.
- **RDS network access**: The RDS instance must never be publicly accessible. Security group rules must restrict port 5432 to the ECS tasks security group only.

## Success Metrics

1. **`terraform plan` succeeds**: Running `terraform plan` from `envs/dev/` exits with code 0 and shows all expected resources across the three modules with no errors or warnings.
2. **`terraform apply` succeeds**: Running `terraform apply` from `envs/dev/` completes without errors, creating all ECR, Fargate, RDS, IAM, ALB, SSM, and CloudWatch resources.
3. **Application reachable**: A `curl` or browser request to `http://<alb-dns-name>/actuator/health` returns HTTP 200 after the ECS service reaches its desired count, confirming end-to-end functionality.

## Open Questions

No open questions at this time.
