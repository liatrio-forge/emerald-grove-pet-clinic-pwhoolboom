# 16-spec-conventional-pr-title.md

## Introduction/Overview

This feature adds a GitHub Actions workflow that validates pull request titles against the [Conventional Commits specification](https://www.conventionalcommits.org/). When a developer opens or updates a pull request targeting the `main` branch, the workflow runs automatically and sets a commit status check indicating whether the title is valid. This prevents non-conforming titles from being merged and ensures commit history stays clean and machine-readable.

## Goals

- Automatically validate PR titles on every relevant pull request event targeting `main`
- Fail the status check when a title does not follow the Conventional Commits format, giving developers clear feedback
- Use the `liatrio/github-actions/conventional-pr-title` action at the pinned commit `0b41561cca6822cc8d880fe0e49e7807a41fdf91`
- Require no additional secrets beyond the built-in `GITHUB_TOKEN`
- Integrate cleanly alongside existing workflows without interference

## User Stories

- **As a developer**, I want my PR title validated against Conventional Commits so that I receive immediate feedback if my title is incorrectly formatted before requesting a review.
- **As a maintainer**, I want all PRs targeting `main` to have a passing title check so that the squash-merge commit history remains consistent and tooling (e.g., changelogs, semantic-release) can parse it reliably.
- **As a contributor**, I want a clear status check on my PR so that I know exactly whether my title is acceptable without reading documentation.

## Demoable Units of Work

### Unit 1: Workflow File Exists and Triggers Correctly

**Purpose:** A new workflow file is present and GitHub recognizes it, firing on the correct events for PRs targeting `main`.

**Functional Requirements:**

- The system shall include a new file at `.github/workflows/conventional-pr-title.yml`
- The workflow shall trigger on pull request events: `opened`, `edited`, `synchronize`, and `reopened`
- The workflow shall only run for pull requests whose base branch is `main`
- The workflow shall use `runs-on: ubuntu-latest`

**Proof Artifacts:**

- `Screenshot: GitHub Actions tab showing the "Conventional PR Title" workflow listed` demonstrates the workflow is recognized by GitHub
- `Screenshot: Workflow run triggered by opening a test PR against main` demonstrates correct trigger behavior

### Unit 2: Valid PR Title Passes the Check

**Purpose:** A PR whose title follows Conventional Commits (e.g., `feat: add login page`) receives a passing status check.

**Functional Requirements:**

- The system shall set a passing commit status when the PR title conforms to the Conventional Commits format
- The system shall display the default success message: "Title follows the specification"
- The status check shall appear on the PR's checks list under the name `conventional-pr-title`

**Proof Artifacts:**

- `Screenshot: PR with title "feat: add login page" showing a green passing status check` demonstrates a valid title is accepted
- `Screenshot: Status check detail showing "Title follows the specification"` demonstrates the correct success message

### Unit 3: Invalid PR Title Fails the Check

**Purpose:** A PR whose title does not follow Conventional Commits (e.g., `Update stuff`) receives a failing status check, giving the developer clear feedback.

**Functional Requirements:**

- The system shall set a failing commit status when the PR title does not conform to the Conventional Commits format
- The system shall display the default failure message: "Title does not follow the specification"
- The status check shall link to the Conventional Commits specification as the details URL

**Proof Artifacts:**

- `Screenshot: PR with title "Update stuff" showing a red failing status check` demonstrates an invalid title is rejected
- `Screenshot: Status check detail showing "Title does not follow the specification" with a link to the spec` demonstrates the correct failure message and detail link

## Non-Goals (Out of Scope)

1. **Branch protection rules**: Configuring GitHub repository settings to enforce the status check as a required check is out of scope — this spec covers only the workflow file itself.
2. **Commit message validation**: Validating individual commit messages (not just the PR title) is out of scope.
3. **Custom type/scope allowlists**: Restricting which Conventional Commits types or scopes are allowed (e.g., only `feat`, `fix`) is out of scope; the default preset is used.
4. **Comment posting**: Posting a comment on the PR explaining the failure is out of scope; the status check message is sufficient.
5. **PRs targeting non-main branches**: Validation only applies to PRs targeting `main`; feature-branch-to-feature-branch PRs are not validated.

## Design Considerations

No specific UI/UX design requirements. The output is a GitHub commit status check, which is rendered natively by GitHub on the PR page. The workflow name should be human-readable (e.g., `Conventional PR Title`) so it is easily identifiable in the Actions tab and on the PR checks list.

## Repository Standards

- **Workflow file location**: `.github/workflows/` following the pattern of existing workflows
- **Action pinning**: Use full SHA-pinned action reference (`liatrio/github-actions/conventional-pr-title@0b41561cca6822cc8d880fe0e49e7807a41fdf91`) consistent with the project requirement
- **Runner**: `ubuntu-latest` consistent with all existing workflows (`e2e-tests.yml`, `claude.yml`, `claude-code-review.yml`)
- **Checkout**: Use `actions/checkout@v4` consistent with existing workflows (only needed if the action requires it; otherwise omit to keep the workflow minimal)
- **Conventional Commits**: The repository already follows Conventional Commits conventions per the git log history

## Technical Considerations

- The `liatrio/github-actions/conventional-pr-title` action requires **`statuses: write`** permission to post the commit status check; this must be declared in the workflow's `permissions` block.
- The action accepts a `token` input that should be set to `${{ secrets.GITHUB_TOKEN }}` — no additional secrets are needed.
- The `pull_request` event (not `pull_request_target`) is appropriate here because no untrusted code is executed; `pull_request_target` would only be needed for cross-fork write access.
- Filtering to `main` is done via the `branches` filter on the `pull_request` trigger, not via a job-level `if` condition, so GitHub correctly reports a skipped check (not a missing check) for non-main PRs.

## Security Considerations

- The workflow uses only `${{ secrets.GITHUB_TOKEN }}`, which is automatically provided by GitHub and scoped to the repository — no additional secrets management is required.
- The `permissions` block should be minimal: `statuses: write` (required) and `pull-requests: read` (to read the PR title). No `contents: write` or other elevated permissions are needed.
- Because this workflow uses `pull_request` (not `pull_request_target`), it runs in the context of the PR head branch with read-only access to secrets from forks, which is the safe default.

## Success Metrics

1. **Workflow present**: The file `.github/workflows/conventional-pr-title.yml` exists in the repository
2. **Valid title passes**: A PR with a conforming title (e.g., `feat: add new feature`) shows a green status check on GitHub
3. **Invalid title fails**: A PR with a non-conforming title (e.g., `my changes`) shows a red status check on GitHub with the message "Title does not follow the specification"

## Open Questions

No open questions at this time.
