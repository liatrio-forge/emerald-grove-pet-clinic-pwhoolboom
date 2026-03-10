# 17 Questions Round 1 - ECR Push Workflow

Please answer each question below (select one or more options, or add your own notes). Feel free to add additional context under any question.

## 1. Workflow Trigger

When should the Docker image be built and pushed to ECR?

- [x] (A) On every push to `main` only
- [ ] (B) On every push to `main` AND on release tags (e.g., `v1.2.3`)
- [ ] (C) On release tags only (aligns with the existing `release.yaml`)
- [ ] (D) Manually via `workflow_dispatch` only
- [ ] (E) Other (describe)

## 2. Image Tagging Strategy

How should the pushed Docker image(s) be tagged in ECR?

- [ ] (A) Git SHA only (e.g., `abc1234`)
- [ ] (B) `latest` only
- [x] (C) Semantic version from release tag (e.g., `v1.2.3`) + `latest`
- [ ] (D) Git SHA + `latest`
- [ ] (E) Other (describe)

## 3. AWS Role ARN

How should the IAM role ARN (used for `aws-actions/configure-aws-credentials` assume-role) be provided?

- [x] (A) As a GitHub Actions secret (e.g., `${{ secrets.AWS_ROLE_ARN }}`)
- [ ] (B) As a GitHub Actions variable (e.g., `${{ vars.AWS_ROLE_ARN }}`)
- [ ] (C) Hardcoded in the workflow (not recommended, but possible for demo purposes)
- [ ] (D) Other (describe)

## 4. ECR Repository Name

What should the ECR repository name be for this image?

- [ ] (A) `emerald-grove-pet-clinic` (matches the project name)
- [ ] (B) `spring-petclinic` (matches the Maven artifact ID)
- [x] (C) Configurable via a GitHub secret or variable
- [ ] (D) Other (describe)

## 5. Relationship to Existing `release.yaml`

Should this ECR push workflow be a separate workflow file, or integrated into the existing `release.yaml`?

- [ ] (A) Separate workflow file (e.g., `publish.yaml` or `docker-ecr.yaml`)
- [ ] (B) Integrated as a new job in the existing `release.yaml`
- [x] (C) Separate workflow that triggers on completion of `release.yaml`
- [ ] (D) Other (describe)

## 6. Multi-Architecture Builds

Should the image be built for multiple CPU architectures?

- [x] (A) No — `linux/amd64` only (simplest, fastest)
- [ ] (B) Yes — `linux/amd64` and `linux/arm64` (broader compatibility)
- [ ] (C) Other (describe)
