# 17-tasks-ecr-push-workflow.md

## Relevant Files

- `.github/workflows/publish.yaml` - New workflow file to create; the primary deliverable of this spec.
- `.github/workflows/release.yaml` - Existing release workflow; use as the formatting reference for pinned SHAs, permissions blocks, concurrency, and `./mvnw` conventions.
- `pom.xml` - Contains the `spring-boot-maven-plugin` configuration; the `build-image` goal is already available and does not require changes.

### Notes

- There are no Java source files to modify for this feature — it is a CI/CD workflow file only.
- The `check-yaml` pre-commit hook will automatically validate the YAML syntax of `publish.yaml` on commit.
- The `gitlint` pre-commit hook enforces conventional commit messages; use the `ci:` prefix (e.g., `ci: add ECR publish workflow`).
- All `uses:` action references must include a full-length pinned SHA with a version comment (e.g., `# v4`). Look up the current SHA for each action on its GitHub releases page before writing the step.
- Do not use `AWS_ACCESS_KEY_ID` or `AWS_SECRET_ACCESS_KEY` anywhere in the workflow.

## Tasks

### [x] 1.0 Create workflow skeleton with trigger, permissions, and Java setup

#### 1.0 Proof Artifact(s)

- YAML lint: `check-yaml` pre-commit hook passes on `.github/workflows/publish.yaml` demonstrates the file is syntactically valid
- Diff: `.github/workflows/publish.yaml` exists with `on: release: types: [published]`, a `concurrency` group, minimal `permissions` blocks, pinned `actions/checkout`, and `actions/setup-java` step demonstrates the scaffold is correct and follows repo conventions

#### 1.0 Tasks

- [x] 1.1 Create `.github/workflows/publish.yaml` with `name: Publish`
- [x] 1.2 Add the trigger block:

  ```yaml
  on:
    release:
      types: [published]
    workflow_dispatch: {}
  ```

- [x] 1.3 Add a `concurrency` group to prevent overlapping runs (follow the same pattern as `release.yaml`: `group: ${{ github.workflow }}`)
- [x] 1.4 Add a top-level `permissions: contents: read` block (deny-by-default, matching `release.yaml` style)
- [x] 1.5 Add a single job named `publish` with `runs-on: ubuntu-latest` and a job-level `permissions` block granting `id-token: write` and `contents: read` (the minimum required for OIDC)
- [x] 1.6 Add a `uses: actions/checkout@<pinned-sha> # v4` step — look up the current pinned SHA from the `actions/checkout` releases page; use the same SHA already in `release.yaml` if it is still current
- [x] 1.7 Add a `Set up JDK 17` step using `actions/setup-java` with a pinned SHA (look up from the `actions/setup-java` releases page), `java-version: '17'`, `distribution: 'temurin'`, and `cache: maven`

---

### [x] 2.0 Add OIDC role assumption and ECR login

#### 2.0 Proof Artifact(s)

- GitHub Actions run log: the `Configure AWS Credentials` step completes without error and the `Login to Amazon ECR` step outputs `Login Succeeded` demonstrates OIDC-based authentication works end-to-end with no static credentials

#### 2.0 Tasks

- [x] 2.1 Look up the current full-length pinned SHA for `aws-actions/configure-aws-credentials` (v6.0.0) from its GitHub releases page
- [x] 2.2 Add a `Configure AWS Credentials` step using `aws-actions/configure-aws-credentials@8df5847569e6427dd6c4fb1cf565c83acfa8afa7 # v6.0.0` with:
  - `role-to-assume: ${{ secrets.AWS_ROLE_ARN }}`
  - `aws-region: ${{ secrets.AWS_REGION }}`
  - `role-session-name: github-actions-ecr-publish`
- [x] 2.3 Look up the current full-length pinned SHA for `aws-actions/amazon-ecr-login` (v2.0.1) from its GitHub releases page
- [x] 2.4 Add a `Login to Amazon ECR` step using `aws-actions/amazon-ecr-login@062b18b96a7aff071d4dc91bc00c4c1a7945b076 # v2.0.1` and capture its `registry` output with an `id: login-ecr` so subsequent steps can reference `${{ steps.login-ecr.outputs.registry }}`

---

### [x] 3.0 Build Docker image and push semver + latest tags to ECR

#### 3.0 Proof Artifact(s)

- GitHub Actions run log: both `docker push` commands complete with image digest output demonstrates dual-tag publication succeeded
- ECR console screenshot (redact AWS account ID from the URL/ARN): repository shows two new image tags — one matching the release semver (e.g., `v1.2.3`) and one `latest` — demonstrates the tagging strategy is correct

#### 3.0 Tasks

- [x] 3.1 Add an `env` block at the job level (or step level) defining:
  - `ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}`
  - `ECR_REPOSITORY: ${{ vars.ECR_REPOSITORY }}`
  - `IMAGE_TAG: ${{ github.event.release.tag_name }}`
- [x] 3.2 Add a `Build Docker image` step that builds and tags the image for ECR in one command:

  ```bash
  ./mvnw --batch-mode spring-boot:build-image \
    -Dspring-boot.build-image.imageName=$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
  ```

  This overrides the default `docker.io/library/spring-petclinic` name so the image is already named correctly for ECR.
- [x] 3.3 Add a `Tag image as latest` step that runs:

  ```bash
  docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG \
              $ECR_REGISTRY/$ECR_REPOSITORY:latest
  ```

- [x] 3.4 Add a `Push semver tag to ECR` step that runs:

  ```bash
  docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
  ```

- [x] 3.5 Add a `Push latest tag to ECR` step that runs:

  ```bash
  docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest
  ```

- [x] 3.6 Commit the completed `publish.yaml` with message `ci: add ECR publish workflow` and open a pull request against `main`
