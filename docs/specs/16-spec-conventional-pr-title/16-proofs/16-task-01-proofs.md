# 16 Task 1.0 Proofs - Create Conventional PR Title Workflow File

## File Existence

The workflow file was created at `.github/workflows/conventional-pr-title.yml`.

## File Content

```yaml
name: Conventional PR Title

on:
  pull_request:
    types: [opened, edited, synchronize, reopened]
    branches: [main]

jobs:
  validate-pr-title:
    runs-on: ubuntu-latest
    permissions:
      statuses: write
      pull-requests: read

    steps:
      - name: Validate PR title
        uses: liatrio/github-actions/conventional-pr-title@0b41561cca6822cc8d880fe0e49e7807a41fdf91
        with:
          token: ${{ secrets.GITHUB_TOKEN }}
```

## Verification

| Requirement | Status | Evidence |
|---|---|---|
| Trigger events: `opened`, `edited`, `synchronize`, `reopened` | ✅ | `types: [opened, edited, synchronize, reopened]` in `on.pull_request` |
| Branch filter: `main` only | ✅ | `branches: [main]` in `on.pull_request` |
| Pinned action SHA | ✅ | `liatrio/github-actions/conventional-pr-title@0b41561cca6822cc8d880fe0e49e7807a41fdf91` |
| Minimal permissions | ✅ | `statuses: write` and `pull-requests: read` only |
| Uses `GITHUB_TOKEN` | ✅ | `token: ${{ secrets.GITHUB_TOKEN }}` |
| Runs on `ubuntu-latest` | ✅ | `runs-on: ubuntu-latest` |
