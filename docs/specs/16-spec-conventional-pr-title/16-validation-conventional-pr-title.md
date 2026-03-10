# 16 Validation Report - Conventional PR Title Workflow

**Validation Completed:** 2026-03-10
**Validation Performed By:** Claude Sonnet 4.6
**Spec:** `docs/specs/16-spec-conventional-pr-title/16-spec-conventional-pr-title.md`
**Task List:** `docs/specs/16-spec-conventional-pr-title/16-tasks-conventional-pr-title.md`
**Branch:** `gha`
**Commit Analyzed:** `72b5205` — `ci: add conventional PR title validation workflow`

---

## 1. Executive Summary

### Overall: FAIL

Gates tripped: **GATE B** (Unknown entries in Coverage Matrix), **GATE C** (Proof artifacts for Tasks 2.0 and 3.0 are absent)

**Implementation Ready: No** — The static workflow file is correctly authored and all
file-level requirements are verified. However, runtime validation via live GitHub Actions
runs has not yet been performed: Tasks 2.0 and 3.0 remain open and their required
screenshots are missing, leaving 6 of 10 functional requirements in `Unknown` state.

**Key Metrics:**

| Metric | Value |
|---|---|
| Functional Requirements Verified | 4 / 10 (40%) |
| Proof Artifacts Working | 1 / 3 (33%) |
| Files Changed | 1 |
| Files Expected (Relevant Files) | 1 |
| Tasks Complete | 1 / 3 (33%) |

---

## 2. Coverage Matrix

### Functional Requirements

| ID | Requirement | Status | Evidence |
|---|---|---|---|
| FR-1 | File `.github/workflows/conventional-pr-title.yml` shall exist | Verified | File present at expected path; commit `72b5205` |
| FR-2 | Workflow triggers on `opened`, `edited`, `synchronize`, `reopened` | Verified | `types: [opened, edited, synchronize, reopened]` in workflow file L5 |
| FR-3 | Workflow only runs for PRs targeting `main` | Verified | `branches: [main]` in workflow file L6 |
| FR-4 | Workflow uses `runs-on: ubuntu-latest` | Verified | `runs-on: ubuntu-latest` in workflow file L10 |
| FR-5 | Passing commit status set when PR title conforms | Unknown | Requires live GitHub Actions run — no screenshot proof yet |
| FR-6 | Success message: "Title follows the specification" | Unknown | Requires live GitHub Actions run — no screenshot proof yet |
| FR-7 | Status check appears under name `conventional-pr-title` | Unknown | Requires live GitHub Actions run — no screenshot proof yet |
| FR-8 | Failing commit status set when PR title does not conform | Unknown | Requires live GitHub Actions run — no screenshot proof yet |
| FR-9 | Failure message: "Title does not follow the specification" | Unknown | Requires live GitHub Actions run — no screenshot proof yet |
| FR-10 | Status check links to Conventional Commits specification | Unknown | Requires live GitHub Actions run — no screenshot proof yet |

### Repository Standards

| Standard Area | Status | Evidence & Compliance Notes |
|---|---|---|
| Workflow file location | Verified | Created at `.github/workflows/` matching `e2e-tests.yml`, `claude.yml`, `claude-code-review.yml` |
| Action pinning | Verified | Full SHA `@0b41561cca6822cc8d880fe0e49e7807a41fdf91` used — no floating tag |
| Runner | Verified | `ubuntu-latest` matches all existing workflows |
| Checkout step omitted (correctly) | Verified | Spec notes checkout is optional; omitting is correct since action only needs the token |
| Conventional Commits commit message | Verified | Commit `72b5205` uses `ci: add conventional PR title validation workflow` |
| Minimal permissions block | Verified | `statuses: write` + `pull-requests: read` only — no excessive permissions |
| Uses `pull_request` (not `pull_request_target`) | Verified | Correct for same-repo branches; no security risk |

### Proof Artifacts

| Task | Proof Artifact | Status | Verification Result |
|---|---|---|---|
| 1.0 | File: `.github/workflows/conventional-pr-title.yml` with correct content | Verified | File exists; content matches spec requirements exactly; `16-task-01-proofs.md` present |
| 2.0 | Screenshot: green `conventional-pr-title` check — "Title follows the specification" | Failed | Screenshot not captured; Task 2.0 marked `[ ]` in task list; no proof file exists |
| 3.0 | Screenshot: red `conventional-pr-title` check — "Title does not follow the specification" | Failed | Screenshot not captured; Task 3.0 marked `[ ]` in task list; no proof file exists |

---

## 3. Validation Issues

| Severity | Issue | Impact | Recommendation |
|---|---|---|---|
| HIGH | **Tasks 2.0 and 3.0 not completed.** Six functional requirements (FR-5 through FR-10) covering runtime behavior remain `Unknown`. No screenshots have been saved to `docs/specs/16-spec-conventional-pr-title/16-proofs/`. | Runtime behavior of the workflow (pass and fail paths) cannot be verified — GATE B and GATE C both fail | Open a PR against `main` with this branch, observe the `Conventional PR Title` check, capture screenshots for both a passing title and a failing title, save them to `16-proofs/`, and update Tasks 2.0 and 3.0 to `[x]` |
| LOW | **Workflow name in YAML casing differs from status check name.** The `name` field is `Conventional PR Title` (display name) while the action sets the status context as `conventional-pr-title` (lowercase-hyphen). These are distinct: one is the Actions workflow name, the other is the commit status context name. This is expected behaviour but worth confirming visually from the screenshot proof. | Minor — no functional impact, but should be verified in the proof screenshots | Confirm in the Task 2.0/3.0 screenshots that the status check context appears as `conventional-pr-title` as required by FR-7 |

---

## 4. Evidence Appendix

### Git Commit Analyzed

```text
72b5205 ci: add conventional PR title validation workflow
  - triggers on opened, edited, synchronize, reopened events
  - applies only to PRs targeting main branch
  - uses liatrio/github-actions/conventional-pr-title@0b41561cca6822cc8d880fe0e49e7807a41fdf91
  - minimal permissions: statuses: write, pull-requests: read
  Related to T1 in Spec 16

Files changed:
  .github/workflows/conventional-pr-title.yml         (new, 19 lines)
  docs/specs/16-spec-conventional-pr-title/16-proofs/16-task-01-proofs.md  (new)
  docs/specs/16-spec-conventional-pr-title/16-questions-1-conventional-pr-title.md (new)
  docs/specs/16-spec-conventional-pr-title/16-spec-conventional-pr-title.md (new)
  docs/specs/16-spec-conventional-pr-title/16-tasks-conventional-pr-title.md (new)
```

### File Comparison: Expected vs Actual

| File | In "Relevant Files"? | Changed? | Result |
|---|---|---|---|
| `.github/workflows/conventional-pr-title.yml` | Yes | Yes | Pass |

Spec/task/questions/proofs files are supporting documentation — they are not listed in
"Relevant Files" but their presence in the commit is fully justified as standard SDD
workflow artifacts. No out-of-scope files were modified.

### Workflow File Content Verification

```yaml
name: Conventional PR Title          # ✅ human-readable workflow name

on:
  pull_request:
    types: [opened, edited, synchronize, reopened]  # ✅ FR-2: all four events
    branches: [main]                                 # ✅ FR-3: main only

jobs:
  validate-pr-title:
    runs-on: ubuntu-latest            # ✅ FR-4: ubuntu-latest
    permissions:
      statuses: write                 # ✅ required by action
      pull-requests: read             # ✅ minimal only

    steps:
      - name: Validate PR title
        uses: liatrio/github-actions/conventional-pr-title@0b41561cca6822cc8d880fe0e49e7807a41fdf91  # ✅ pinned SHA
        with:
          token: ${{ secrets.GITHUB_TOKEN }}         # ✅ no extra secrets
```

### Pre-commit Hook Results (from commit log)

All hooks passed on the final commit:

- `trim trailing whitespace` — Passed
- `fix end of files` — Passed
- `check yaml` — Passed
- `markdownlint` — Passed (auto-fixed then passed on retry)
- `safety`, `shellcheck`, `Maven compilation check` — Skipped (not applicable)

### Missing Proof Files

```text
docs/specs/16-spec-conventional-pr-title/16-proofs/
├── 16-task-01-proofs.md  ✅ present
├── 16-task-02-proofs.md  ❌ missing — Task 2.0 screenshot not captured
└── 16-task-03-proofs.md  ❌ missing — Task 3.0 screenshot not captured
```
