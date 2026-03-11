# 17 Task 3.0 Proofs — Docker Build and Push (Semver + Latest)

## Task

Build Docker image and push semver + latest tags to ECR.

## Workflow Steps Added

```yaml
- name: Build Docker image
  env:
    ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
    ECR_REPOSITORY: ${{ vars.ECR_REPOSITORY }}
    IMAGE_TAG: ${{ github.event.release.tag_name }}
  run: |
    ./mvnw --batch-mode spring-boot:build-image \
      -Dspring-boot.build-image.imageName=$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG

- name: Tag image as latest
  env:
    ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
    ECR_REPOSITORY: ${{ vars.ECR_REPOSITORY }}
    IMAGE_TAG: ${{ github.event.release.tag_name }}
  run: |
    docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG \
               $ECR_REGISTRY/$ECR_REPOSITORY:latest

- name: Push semver tag to ECR
  env:
    ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
    ECR_REPOSITORY: ${{ vars.ECR_REPOSITORY }}
    IMAGE_TAG: ${{ github.event.release.tag_name }}
  run: docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG

- name: Push latest tag to ECR
  env:
    ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
    ECR_REPOSITORY: ${{ vars.ECR_REPOSITORY }}
  run: docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest
```

## Expected Runtime Proof

When triggered by a published release (e.g., `v1.2.3`), the Actions log will show:

```text
# Push semver tag to ECR
The push refers to repository [123456789012.dkr.ecr.us-east-1.amazonaws.com/emerald-grove-pet-clinic]
v1.2.3: digest: sha256:<digest> size: <size>

# Push latest tag to ECR
The push refers to repository [123456789012.dkr.ecr.us-east-1.amazonaws.com/emerald-grove-pet-clinic]
latest: digest: sha256:<digest> size: <size>
```

ECR console will show two new tags for the repository: `v1.2.3` and `latest`.
(Screenshot: redact AWS account ID from the ECR console URL before committing.)

## Design Notes

- `spring-boot:build-image` uses Cloud Native Buildpacks and names the image directly
  as `<ecr-registry>/<repo>:<tag>` via `-Dspring-boot.build-image.imageName`, avoiding
  a separate `docker tag` step for the semver tag.
- `vars.ECR_REPOSITORY` is a non-sensitive GitHub Actions variable (not a secret).
- `github.event.release.tag_name` provides the semver from the GitHub Release event,
  which is populated when `release.yaml` calls `gh release create`.

## Verification

| Requirement | Status |
|---|---|
| Trigger is `release: types: [published]` — fires after `release.yaml` | ✅ |
| `IMAGE_TAG` sourced from `github.event.release.tag_name` | ✅ |
| `ECR_REPOSITORY` sourced from `vars.ECR_REPOSITORY` | ✅ |
| Image built with correct ECR URI as name (no re-tag needed for semver) | ✅ |
| `latest` tag created via `docker tag` | ✅ |
| Both `$IMAGE_TAG` and `latest` pushed separately | ✅ |
| `./mvnw` used (not bare `mvn`) | ✅ |
| `--batch-mode` flag on Maven command | ✅ |
