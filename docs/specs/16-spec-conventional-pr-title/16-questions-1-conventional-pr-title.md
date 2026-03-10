# 16 Questions Round 1 - Conventional PR Title Workflow

Please answer each question below (select one or more options, or add your own notes). Feel free to add additional context under any question.

## 1. Workflow Trigger Events

Which pull request events should trigger the PR title validation check?

- [x] (A) `opened`, `edited`, `synchronize`, `reopened` — validate on creation, title edits, new commits, and re-opens (most thorough coverage)
- [ ] (B) `opened`, `edited`, `reopened` only — skip `synchronize` since the title doesn't change when new commits are pushed
- [ ] (C) `opened` and `edited` only — minimal set covering title creation and changes
- [ ] (D) Other (describe)

## 2. Failure Behavior

What should happen when a PR title does not follow the Conventional Commits format?

- [x] (A) Fail the status check (block merging if branch protection is configured) — this is the default behavior of the action
- [ ] (B) Post a warning comment on the PR in addition to failing the check
- [ ] (C) Only post a comment; do not fail the status check
- [ ] (D) Other (describe)

## 3. Custom Status Check Messages

Should the status check use custom pass/fail messages, or the action defaults?

- [x] (A) Use the action defaults: "Title follows the specification" / "Title does not follow the specification"
- [ ] (B) Use custom messages (e.g., "PR title is valid" / "PR title must follow Conventional Commits: feat: ..., fix: ..., etc.")
- [ ] (C) Other (describe)

## 4. Scope of Application

Should this workflow run on all pull requests, or only a subset?

- [ ] (A) All pull requests targeting any branch
- [x] (B) Only pull requests targeting the `main` branch
- [ ] (C) Only pull requests from non-maintainer contributors (e.g., filter by `author_association`)
- [ ] (D) Other (describe)
