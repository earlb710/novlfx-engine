# Copilot guidance for novlfx-engine

## Validation strategy

- Prefer lightweight validation for small changes, such as `./gradlew --no-daemon compileJava` or targeted test classes.
- Use broader validation such as `./gradlew --no-daemon build` for large, risky, or cross-cutting changes.
- Do not launch the manual JavaFX sample applications as part of routine automated validation unless the task specifically requires it.

## MCP usage

- Keep GitHub MCP tools enabled for repository context, issues, pull requests, workflow runs, and CI log investigation.
- Do not commit MCP tokens, Copilot tokens, GitHub tokens, or other credentials to the repository.
- Store required Copilot or MCP credentials as GitHub environment secrets or variables, preferably under the `copilot` environment.

## Copilot setup

- The Copilot setup workflow prepares Java 17 and warms Gradle dependencies so agents can compile quickly without running the full test suite up front.
- If Copilot tasks become slow because of JavaFX or Gradle dependency resolution, consider moving the setup job to a larger Ubuntu runner.
