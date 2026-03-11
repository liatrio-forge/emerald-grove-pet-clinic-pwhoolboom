# Task 2.0 Proof Artifact — Create RDS Module

## terraform validate Output

```text
$ cd infra/modules/rds && terraform init -backend=false && terraform validate
...
Terraform has been successfully initialized!
...
Success! The configuration is valid.
```

Full output from `terraform validate`:

```text
Success! The configuration is valid.
```

Exit code: 0

## Files Created

All files created under `infra/modules/rds/`:

| File | Description |
|------|-------------|
| `infra/modules/rds/variables.tf` | Input variables: project_name, environment, aws_region, aws_account_id, ecs_security_group_id, db_name, db_username, platform_state_bucket/key/region |
| `infra/modules/rds/remote_state.tf` | Platform remote state data source; locals for data_subnet_ids and vpc_id |
| `infra/modules/rds/security.tf` | RDS security group allowing port 5432 from ECS tasks security group (using security_groups attribute) |
| `infra/modules/rds/rds.tf` | random_password, aws_db_subnet_group, and aws_db_instance (PostgreSQL 15.12, db.t3.micro, gp3, encrypted) |
| `infra/modules/rds/ssm.tf` | SSM SecureString parameters for db/url, db/username, db/password |
| `infra/modules/rds/outputs.tf` | Outputs: rds_endpoint, rds_security_group_id, ssm_db_url_arn, ssm_db_username_arn, ssm_db_password_arn |

## Notes

- The `aws_security_group` inline ingress block does not support `source_security_group_id`; the correct attribute is `security_groups = [var.ecs_security_group_id]`. This was corrected during validation.
- The `aws_ssm_parameter` resource does not support a `sensitive` argument; the `sensitive` handling for password output is managed at the outputs level rather than on the resource itself.
- Provider versions installed: hashicorp/aws v6.35.1, hashicorp/random v3.0.1
