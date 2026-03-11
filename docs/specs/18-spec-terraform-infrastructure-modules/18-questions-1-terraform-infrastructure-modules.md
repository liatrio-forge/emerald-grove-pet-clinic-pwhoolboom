# 18 Questions Round 1 - Terraform Infrastructure Modules

Please answer each question below (select one or more options, or add your own notes). Feel free to add additional context under any question.

## 1. Module Structure Approach

Should the infrastructure be organized as a single all-in-one module (like the reference) or split into separate, composable modules (one for ECR, one for Fargate/ECS, one for RDS)?

- [ ] (A) Single monolithic module — everything in one `app-infrastructure` module, matching the reference structure
- [x] (B) Separate modules — individual modules for `ecr`, `fargate`, and `rds`, composed together at the env level
- [ ] (C) Hybrid — a few focused modules (e.g., `networking`, `compute`, `database`) grouped by concern
- [ ] (D) Other (describe)

## 2. Environments

Which environments should be created under `envs/`?

- [x] (A) `dev` only — match the reference, single environment for now
- [ ] (B) `dev` and `prod` — both environments, matching the reference exactly
- [ ] (C) `dev` and `staging` — use staging instead of prod
- [ ] (D) Other (describe)

## 3. ECR Repository Naming

The reference hardcodes the ECR repo name as `pet-clinic-toddwells`. What should the new repo be named?

- [x] (A) `pet-clinic-pwhoolboom` — direct replacement of `toddwells` with `pwhoolboom`
- [ ] (B) Use a variable/dynamic name based on `project_name` (e.g., `pet-clinic-${var.project_name}`)
- [ ] (C) Keep the same pattern but let it be fully configurable via a variable
- [ ] (D) Other (describe)

## 4. SSM Parameter Path Naming

The reference uses `/pet-clinic-twells/${environment}/...` for SSM paths (and hardcodes this in IAM policies). What should the new path prefix be?

- [x] (A) `/pet-clinic-pwhoolboom/${environment}/...` — direct replacement
- [ ] (B) Use a variable-driven prefix (e.g., `/${var.project_name}/${environment}/...`) for full flexibility
- [ ] (C) Match the ECR naming choice from question 3
- [ ] (D) Other (describe)

## 5. Terraform State Backend

The reference uses S3 + DynamoDB for remote state. What should the new state backend configuration look like?

- [x] (A) Same S3 buckets as the reference (`terraform-state-ecs-landingzone-dev` / `terraform-state-ecs-landingzone`) but with a new key path (`apps/pet-clinic-pwhoolboom/...`)
- [ ] (B) Different S3 buckets entirely — specify bucket names in the answers
- [ ] (C) Leave backend configuration as a placeholder (e.g., empty `backend "s3" {}` with a comment)
- [ ] (D) Other (describe)

## 6. ALB Inclusion

The reference module includes an Application Load Balancer alongside Fargate. Should this new module suite also include the ALB?

- [x] (A) Yes — include the ALB as part of the Fargate/compute module, matching the reference
- [ ] (B) No — omit the ALB; expose the ECS service directly or via another mechanism
- [ ] (C) Make it optional via a variable flag (`enable_alb = true/false`)
- [ ] (D) Other (describe)

## 7. Permissions Boundary

The reference enforces `arn:aws:iam::${account_id}:policy/team-permissions-boundary-v1` on all IAM roles. Should this new module do the same?

- [x] (A) Yes — apply the same permissions boundary on all IAM roles
- [ ] (B) No — omit the permissions boundary
- [ ] (C) Make it optional via a variable (e.g., `permissions_boundary_arn = ""`)
- [ ] (D) Other (describe)

## 8. Anthropic API Key Secret

The reference includes an SSM parameter for an Anthropic API key used by the Spring app. Should this be included in the new module?

- [x] (A) Yes — include the Anthropic API key SSM parameter with `lifecycle.ignore_changes`, same as the reference
- [ ] (B) No — only include DB-related secrets (URL, username, password)
- [ ] (C) Make secrets configurable — pass in a list of additional SSM parameters to create
- [ ] (D) Other (describe)

## 9. Platform Remote State

The reference reads VPC/subnet/ECS cluster info from a shared platform Terraform state file in S3. Should this new module do the same?

- [ ] (A) Yes — read from the same platform state bucket/key as the reference
- [x] (B) Yes — read from the same platform state but make the bucket/key configurable via variables. Make the defaults the same as the reference.
- [ ] (C) No — accept VPC ID, subnet IDs, and ECS cluster name as direct input variables instead
- [ ] (D) Other (describe)

## 10. Proof Artifacts

What will demonstrate that this infrastructure module suite is working correctly?

- [ ] (A) Successful `terraform plan` output showing all expected resources (ECR, ECS task def, RDS, IAM roles)
- [ ] (B) Successful `terraform apply` in the dev environment with all resources created
- [ ] (C) Screenshots or CLI output showing the deployed ECR repo URL, RDS endpoint, and ECS service status
- [x] (D) All of the above
- [ ] (E) Other (describe)
