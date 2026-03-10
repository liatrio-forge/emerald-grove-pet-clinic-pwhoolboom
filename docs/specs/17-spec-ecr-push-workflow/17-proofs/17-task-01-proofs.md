# 17 Task 1.0 Proofs — Workflow Skeleton

## Task

Create workflow skeleton with trigger, permissions, and Java setup.

## CLI Output

### check-yaml pre-commit hook

```text
$ pre-commit run check-yaml --files .github/workflows/publish.yaml
check yaml...............................................................Passed
```

## Diff

`.github/workflows/publish.yaml` created with all required scaffold elements:

```yaml
name: Publish

on:
  release:
    types: [published]

  workflow_dispatch: {}

concurrency:
  group: ${{ github.workflow }}

permissions:
  contents: read

jobs:
  publish:
    name: publish
    runs-on: ubuntu-latest
    permissions:
      contents: read
      id-token: write

    steps:
      - uses: actions/checkout@08c6903cd8c0fde910a37f88322edcfb5dd907a8 # v5

      - name: Set up JDK 17
        uses: actions/setup-java@be666c2fcd27ec809703dec50e508c2fdc7f6654 # v5.2.0
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
```

## Verification

| Requirement | Status |
|---|---|
| `on: release: types: [published]` trigger present | ✅ |
| `workflow_dispatch` present | ✅ |
| `concurrency` group present | ✅ |
| Top-level `permissions: contents: read` (deny-by-default) | ✅ |
| Job-level `id-token: write` for OIDC | ✅ |
| `actions/checkout` pinned to full SHA with version comment | ✅ |
| `actions/setup-java` pinned to full SHA with version comment | ✅ |
| JDK 17, temurin, Maven cache | ✅ |
| YAML syntax valid (`check-yaml` passed) | ✅ |
