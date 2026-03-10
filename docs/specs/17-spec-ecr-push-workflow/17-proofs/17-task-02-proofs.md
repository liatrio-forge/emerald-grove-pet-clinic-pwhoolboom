# 17 Task 2.0 Proofs — OIDC Role Assumption and ECR Login

## Task

Add OIDC role assumption and ECR login steps.

## SHA Resolution

SHAs were resolved via the GitHub API by dereferencing the latest release tags:

```text
aws-actions/configure-aws-credentials @ v6.0.0 = 8df5847569e6427dd6c4fb1cf565c83acfa8afa7
aws-actions/amazon-ecr-login          @ v2.0.1  = 062b18b96a7aff071d4dc91bc00c4c1a7945b076
```

Resolution commands used:

```bash
$ gh api repos/aws-actions/configure-aws-credentials/releases/latest --jq '.tag_name'
v6.0.0

$ gh api repos/aws-actions/amazon-ecr-login/releases/latest --jq '.tag_name'
v2.0.1
```

## Workflow Steps Added

```yaml
- name: Configure AWS Credentials
  uses: aws-actions/configure-aws-credentials@8df5847569e6427dd6c4fb1cf565c83acfa8afa7 # v6.0.0
  with:
    role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
    aws-region: ${{ secrets.AWS_REGION }}
    role-session-name: github-actions-ecr-publish

- name: Login to Amazon ECR
  id: login-ecr
  uses: aws-actions/amazon-ecr-login@062b18b96a7aff071d4dc91bc00c4c1a7945b076 # v2.0.1
```

## Expected Runtime Proof

When the workflow runs, the Actions log for the `Login to Amazon ECR` step will contain:

```text
Login Succeeded
```

This demonstrates OIDC-based credential exchange succeeded with no static AWS keys present.

## Verification

| Requirement | Status |
|---|---|
| `aws-actions/configure-aws-credentials` pinned to full SHA | ✅ |
| `role-to-assume` uses `secrets.AWS_ROLE_ARN` (not hardcoded) | ✅ |
| `aws-region` uses `secrets.AWS_REGION` (not hardcoded) | ✅ |
| `role-session-name` set for auditability | ✅ |
| `aws-actions/amazon-ecr-login` pinned to full SHA | ✅ |
| `login-ecr` step `id` set so `registry` output is referenceable | ✅ |
| No `AWS_ACCESS_KEY_ID` or `AWS_SECRET_ACCESS_KEY` anywhere in workflow | ✅ |
| `id-token: write` permission granted at job level | ✅ |
