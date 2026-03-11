# 17-spec-ecr-push-workflow.md

## Introduction/Overview

Add a GitHub Actions workflow that builds a Docker image of the Emerald Grove Pet Clinic Spring Boot application and pushes it to Amazon Elastic Container Registry (ECR). Authentication to AWS uses OIDC-based role assumption — no long-lived AWS credentials are stored. The workflow triggers automatically whenever a GitHub Release is published (i.e., after `release.yaml` successfully completes).

## Goals

- Automate Docker image publication to ECR on every successful release
- Use short-lived AWS credentials via IAM role assumption (OIDC), eliminating static AWS access keys
- Tag published images with the release's semantic version (e.g., `v1.2.3`) and `latest`
- Keep the ECR repository name and role ARN configurable via GitHub secrets/variables
- Remain fast and simple: `linux/amd64` only, no matrix builds

## User Stories

**As a platform engineer**, I want the pet clinic image automatically pushed to ECR after each release so that downstream deployment pipelines always have a versioned, up-to-date artifact without manual intervention.

**As a security-conscious operator**, I want AWS authentication to use an assumed IAM role via OIDC so that no long-lived credentials are stored in GitHub secrets.

**As a developer**, I want the published image tagged with both the release semver and `latest` so that I can reference a specific version or always pull the newest release.

## Demoable Units of Work

### Unit 1: OIDC Role Assumption and ECR Login

**Purpose:** Prove that the workflow can authenticate to AWS and log in to ECR using a role assumption — no static credentials required.

**Functional Requirements:**

- The workflow shall request an OIDC token from GitHub and exchange it for temporary AWS credentials using `aws-actions/configure-aws-credentials`
- The workflow shall use the role ARN stored in `secrets.AWS_ROLE_ARN`
- The workflow shall use the AWS region stored in `secrets.AWS_REGION` (or a workflow-level default)
- The workflow shall log in to ECR using `aws-actions/amazon-ecr-login` after credentials are configured
- The workflow shall fail fast and clearly if the role assumption or ECR login fails

**Proof Artifacts:**

- GitHub Actions run log: `Login Succeeded` output from the ECR login step demonstrates successful OIDC-based authentication

### Unit 2: Docker Build and Push with Semver + Latest Tags

**Purpose:** Prove that the correct image is built and pushed to ECR with both the release version tag and `latest`.

**Functional Requirements:**

- The workflow shall trigger on the `release: types: [published]` event (fires after `release.yaml` creates a GitHub Release)
- The workflow shall build a Docker image using Spring Boot's Maven build-image plugin: `./mvnw spring-boot:build-image`
- The workflow shall tag the image as `<ecr-registry>/<repo-name>:<release-tag>` and `<ecr-registry>/<repo-name>:latest`
- The release tag shall be derived from `github.event.release.tag_name`
- The ECR repository name shall be sourced from `vars.ECR_REPOSITORY`
- The workflow shall push both tags to ECR
- The workflow shall require the `id-token: write` permission for OIDC

**Proof Artifacts:**

- GitHub Actions run log: both `docker push` commands succeed with image digest output, demonstrating successful publication
- ECR console (screenshot): repository shows two new tags matching the release version and `latest`

## Non-Goals (Out of Scope)

1. **Multi-architecture builds**: Only `linux/amd64` will be built; `arm64` is excluded
2. **Vulnerability scanning**: No image scanning (e.g., Trivy, ECR native scanning) is configured by this spec
3. **ECR repository creation**: The ECR repository must already exist; this workflow does not create it via Terraform or CLI
4. **Deployment triggering**: This workflow only pushes to ECR; triggering downstream Kubernetes or ECS deployments is out of scope
5. **Non-release branches**: Images are only pushed on published releases, not on every commit to `main`

## Design Considerations

No specific UI/UX requirements. The workflow should follow the same formatting conventions as the existing `.github/workflows/release.yaml` (pinned action SHAs with version comments, `concurrency` group, explicit `permissions` blocks).

## Repository Standards

- **Pinned action SHAs**: All `uses:` references must include a pinned full-length SHA with a version comment (e.g., `# v4`), matching the pattern in `release.yaml`
- **Explicit permissions**: Use minimal `permissions` blocks at both workflow and job level, matching `release.yaml` style
- **Concurrency**: Include a `concurrency` group to prevent overlapping runs
- **Maven wrapper**: Use `./mvnw` (not `mvn`) for all Maven commands, consistent with project scripts
- **Conventional commits**: Workflow file changes should use the `ci:` prefix

## Technical Considerations

- **OIDC trust policy**: The IAM role's trust policy must allow `token.actions.githubusercontent.com` as a federated identity provider, scoped to this repository. This is a prerequisite outside the workflow itself.
- **Spring Boot build-image**: Uses Cloud Native Buildpacks via the Maven plugin (`spring-boot:build-image`). Requires Docker to be available on the runner (`ubuntu-latest` provides this).
- **Trigger mechanism**: Using `on: release: types: [published]` is preferred over `workflow_run` because it provides `github.event.release.tag_name` directly, avoiding the need to parse or pass the version between workflows.
- **Image naming**: The full ECR image URI is constructed as `<aws_account_id>.dkr.ecr.<region>.amazonaws.com/<ECR_REPOSITORY>`, with the registry URL returned by `amazon-ecr-login`.
- **Java setup**: JDK 17 must be configured before running the Maven build, identical to `release.yaml`.

## Security Considerations

- **`secrets.AWS_ROLE_ARN`**: Contains the full IAM role ARN. Must be stored as a GitHub Actions secret, never hardcoded.
- **`secrets.AWS_REGION`**: AWS region for ECR. May be stored as a secret or a variable depending on sensitivity preference.
- **`vars.ECR_REPOSITORY`**: ECR repository name. Non-sensitive; store as a GitHub Actions variable.
- **`id-token: write` permission**: Required for OIDC; must be explicitly granted at the job level.
- **No static AWS keys**: `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` must NOT be used.
- **Proof artifacts**: ECR screenshots should not expose AWS account IDs in public documentation.

## Success Metrics

1. **Automated publish**: Every GitHub Release created by `release.yaml` results in a corresponding ECR push within 5 minutes, with zero manual steps
2. **Dual tags present**: Both `v<semver>` and `latest` tags are visible in ECR after each run
3. **No static credentials**: The workflow contains no `AWS_ACCESS_KEY_ID` or `AWS_SECRET_ACCESS_KEY` references
4. **Clean failure**: If OIDC auth or ECR login fails, the workflow fails at that step with a clear error message before attempting a build

## Open Questions

1. Should `AWS_REGION` be a secret or a variable? (Regions are not sensitive, so `vars.AWS_REGION` may be more appropriate — but left to the implementer's preference.)
2. Should the image name built by `spring-boot:build-image` be explicitly overridden via `-Dspring-boot.build-image.imageName=...`, or is the default acceptable? The default produces `docker.io/library/spring-petclinic:<version>` which would need to be re-tagged before pushing.
