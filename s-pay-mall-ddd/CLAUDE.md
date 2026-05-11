@AGENTS.md

# Claude Code Specific Rules

## Spec workflow

This repository uses `code_copilot/` as the project-level Spec AI workflow directory.

When working on implementation tasks:

1. Start from `code_copilot/README.md`.
2. Read the relevant project rules under `code_copilot/rules/`.
3. If a change directory exists under `code_copilot/changes/`, use it as the source of truth.
4. Do not skip `spec.md` and `tasks.md`.
5. When the task is complete, update the corresponding task status and implementation log if appropriate.

## Claude behavior

- Prefer plan mode for large or risky changes.
- Use small diffs.
- Before editing, explain which spec/change directory you are following.
- If no matching spec exists, propose creating one under `code_copilot/changes/<change-id>/` before implementation.