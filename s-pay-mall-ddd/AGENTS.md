# AGENTS.md

## Project AI Workflow

This project uses `code_copilot/` as the authoritative AI collaboration specification directory.

Before making non-trivial code changes, Codex must follow this workflow:

1. Read `code_copilot/README.md`.
2. Read these project rules:
   - `code_copilot/rules/project-context.md`
   - `code_copilot/rules/domain-rules.md`
   - `code_copilot/rules/coding-style.md`
   - `code_copilot/rules/security.md`
3. If the user mentions a change name, feature name, or task name, locate the matching directory under `code_copilot/changes/<change-id>/`.
4. For that change, read:
   - `spec.md`
   - `tasks.md`
   - `test-spec.md` if present
   - `log.md` if present
5. Do not implement before understanding the spec and tasks.
6. After implementation, update the relevant `tasks.md` and `log.md` when appropriate.
7. Prefer small, reviewable changes.
8. Do not silently change public APIs, database schema, MQ topics, payment/refund behavior, or domain rules without explicitly calling it out.

## Working Style

- Follow DDD boundaries in this project.
- Prefer implementation driven by existing specs.
- If specs and code conflict, stop and explain the conflict before changing code.
- For risky changes, propose a plan first.
- For bug fixes, identify root cause before editing.
- For tests, prefer targeted tests first, then broader regression tests.
