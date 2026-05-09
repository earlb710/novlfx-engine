# Copilot guidance for novlfx-engine

## Validation strategy

- Prefer lightweight validation for small changes, such as `./gradlew --no-daemon compileJava` or targeted test classes.
- Use broader validation such as `./gradlew --no-daemon build` for large, risky, or cross-cutting changes.
- Do not launch the manual JavaFX sample applications as part of routine automated validation unless the task specifically requires it.
- Avoid running the full test suite during setup or routine small changes unless the task requires it.

## MCP usage

- Keep GitHub MCP tools enabled for repository context, releases, issues, pull requests, workflow runs, and CI log investigation.
- Do not commit MCP tokens, Copilot tokens, GitHub tokens, or other credentials to the repository.
- Store required Copilot or MCP credentials as GitHub environment secrets or variables, preferably under the `copilot` environment.
- Do not include real MCP, Copilot, or GitHub tokens in source files, workflow files, documentation examples, or logs.

## Copilot setup

- The Copilot setup workflow prepares Java 17 and warms Gradle dependencies so agents can compile quickly without running the full test suite up front.
- The setup workflow should use minimal permissions, run in the `copilot` environment, and install only the project toolchain and dependency cache needed before the agent starts.
- If Copilot tasks become slow because of JavaFX or Gradle dependency resolution, consider moving the setup job to a larger Ubuntu runner.

## Change summary

- After completing any set of code changes, write a summary of those changes to `change_summary.md` in the repository root.
- If the current branch already has a `change_summary.md`, **append** the new summary to the end of the file.
- If `change_summary.md` does not yet exist on the current branch (i.e. this is the first set of changes on the branch), create the file (or overwrite any version carried over from a different branch) with only the new summary.
- when checking in rename `change_summary.md` to `change_summary_branchname.md` that way there should be no merge conflicts. 

## Private repository access

- Configure access to private repositories only through masked GitHub environment secrets under the `copilot` environment.
- Use Git URL rewriting in setup steps when private repository access is required, but never hard-code tokens in YAML.
