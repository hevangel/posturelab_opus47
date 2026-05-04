# Contributing to PostureLab

## Policy

**Human code contributions are not accepted.** All code changes must be authored by an AI coding agent and submitted as a pull request. Human maintainers review and merge AI-generated PRs but do not write code directly.

### What this means

- Do **not** open a PR with hand-written code — it will be closed immediately.
- Instead, describe the change you want in a GitHub Issue or in a chat session with an AI agent, and let the agent produce the code.
- The human's role is to **specify requirements, review diffs, and approve merges**.

### PR checklist (for AI agents)

1. All existing tests pass (`python tests/test_posture_math.py`, `npm test`, `./gradlew :app:testDebugUnitTest`).
2. New behaviour has at least one new test.
3. Brand parity: if you touch colours/text, update all three brand files (see [AGENTS.md](AGENTS.md)).
4. Posture-math parity: Python and Kotlin implementations stay numerically equivalent.
5. The chat session extract that produced the commit is saved under `chat_sessions/<date>_<slug>.md`.

### Reporting bugs / requesting features

Open a GitHub Issue describing the desired behaviour. An AI agent will pick it up.
