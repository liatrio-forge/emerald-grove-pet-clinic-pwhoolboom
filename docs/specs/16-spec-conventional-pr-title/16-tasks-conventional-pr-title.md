# 16 Tasks - Conventional PR Title Workflow

## Relevant Files

- `.github/workflows/conventional-pr-title.yml` - New workflow file to create; this is the sole deliverable for this feature.

### Notes

- This feature has no application source code changes — implementation is entirely in the new workflow YAML file.
- Validation (Tasks 2.0 and 3.0) requires actual GitHub Actions runs triggered by real pull requests against `main`; there is no local test command.
- Follow the existing workflow conventions in `.github/workflows/` (e.g., `runs-on: ubuntu-latest`, `actions/checkout@v4` usage, pinned action references).
- Commit message must follow Conventional Commits (e.g., `ci: add conventional PR title validation workflow`).

## Tasks

### [x] 1.0 Create the conventional-pr-title GitHub Actions workflow file

#### 1.0 Proof Artifact(s)

- File: `.github/workflows/conventional-pr-title.yml` exists in the repository with correct content demonstrates the workflow is defined
- Diff: The new workflow file shows correct trigger events (`opened`, `edited`, `synchronize`, `reopened`), `branches: [main]` filter, pinned action reference, and minimal permissions block (`statuses: write`, `pull-requests: read`)

#### 1.0 Tasks

- [x] 1.1 Create the file `.github/workflows/conventional-pr-title.yml` and set the top-level `name` field to `Conventional PR Title`
- [x] 1.2 Add the `on:` trigger block: use the `pull_request` event with `types: [opened, edited, synchronize, reopened]` and `branches: [main]`
- [x] 1.3 Add a `jobs:` block with a single job named `validate-pr-title` that runs on `ubuntu-latest`
- [x] 1.4 Add a `permissions:` block to the job with `statuses: write` and `pull-requests: read` (no other permissions needed)
- [x] 1.5 Add a single step that uses the pinned action `liatrio/github-actions/conventional-pr-title@0b41561cca6822cc8d880fe0e49e7807a41fdf91` with `token: ${{ secrets.GITHUB_TOKEN }}`
- [x] 1.6 Commit the file with a conventional commit message (e.g., `ci: add conventional PR title validation workflow`)

### [ ] 2.0 Validate the workflow passes on a conforming PR title

#### 2.0 Proof Artifact(s)

- Screenshot: GitHub PR checks section showing a green "conventional-pr-title" status check with message "Title follows the specification" for a PR titled `feat: add conventional pr title workflow` demonstrates the pass path works end-to-end

#### 2.0 Tasks

- [ ] 2.1 Push the branch containing the new workflow file and open a pull request against `main` with a conforming title (e.g., `feat: add conventional pr title workflow`)
- [ ] 2.2 Wait for the `Conventional PR Title` workflow run to complete in the GitHub Actions tab
- [ ] 2.3 Confirm the PR checks section shows a green status check named `conventional-pr-title` with the message "Title follows the specification"
- [ ] 2.4 Save a screenshot of the passing check to `docs/specs/16-spec-conventional-pr-title/16-proofs/` as proof

### [ ] 3.0 Validate the workflow fails on a non-conforming PR title

#### 3.0 Proof Artifact(s)

- Screenshot: GitHub PR checks section showing a red "conventional-pr-title" status check with message "Title does not follow the specification" for a PR with a non-conforming title (e.g., `Update stuff`) demonstrates the fail path works end-to-end

#### 3.0 Tasks

- [ ] 3.1 Edit the open PR's title to a non-conforming value (e.g., `Update stuff`) to trigger a new workflow run via the `edited` event
- [ ] 3.2 Wait for the new `Conventional PR Title` workflow run to complete in the GitHub Actions tab
- [ ] 3.3 Confirm the PR checks section shows a red status check named `conventional-pr-title` with the message "Title does not follow the specification"
- [ ] 3.4 Save a screenshot of the failing check to `docs/specs/16-spec-conventional-pr-title/16-proofs/` as proof
- [ ] 3.5 Restore the PR title to a conforming value (e.g., `feat: add conventional pr title workflow`) before merging
