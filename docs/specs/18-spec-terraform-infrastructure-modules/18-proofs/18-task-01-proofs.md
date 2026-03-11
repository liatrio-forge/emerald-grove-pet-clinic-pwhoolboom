# Task 1.0 Proof Artifact — Create ECR Module

## Files Created

- `infra/.gitignore` — Terraform-specific ignores
- `infra/modules/ecr/variables.tf` — Input variables: `project_name`, `environment`, `aws_region`
- `infra/modules/ecr/main.tf` — `aws_ecr_repository` and `aws_ecr_lifecycle_policy` resources
- `infra/modules/ecr/outputs.tf` — Outputs: `repository_url`, `repository_name`

## terraform validate Output

Command run from `infra/modules/ecr/`:

```text
$ terraform init -backend=false
Initializing provider plugins...
- Finding latest version of hashicorp/aws...
- Installing hashicorp/aws v6.35.1...
- Installed hashicorp/aws v6.35.1 (signed by HashiCorp)

Terraform has created a lock file .terraform.lock.hcl to record the provider
selections it made above. Include this file in your version control repository
so that Terraform can guarantee to make the same selections by default when
you run "terraform init" in the future.

Terraform has been successfully initialized!

$ terraform validate
Success! The configuration is valid.
```

Exit code: 0
