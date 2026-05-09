# Copilot guidance for novlfx-engine

## Repository overview

- This is a Java 17 Gradle `java-library` project for a reusable JavaFX visual-novel/game engine.
- The Java module is `com.novlfx.engine`; update `src/main/java/module-info.java` when adding exported packages or new module dependencies.
- Main engine code lives under `src/main/java/com/eb/javafx/*`; automated tests and manual support apps live under `src/test/java/com/eb/javafx/*`.
- Public reusable APIs are organized by feature package, including `bootstrap`, `routing`, `scene`, `ui`, `display`, `audio`, `save`, `prefs`, `gamesupport`, `text`, and `util`.
- Keep this repository generic and reusable. Do not add game-specific story content, route names, assets, save schemas, or one-off migration helpers to engine code.

## First files to read

- `build.gradle` and `settings.gradle` for the Java/Gradle toolchain, dependencies, test setup, and manual app tasks.
- `docs/USER_MANUAL.md` for the current user-facing API guide, package summary, validation commands, and manual tool descriptions.
- `examples/user-manual/README.md` for the example snippets that mirror the user manual sections.
- `docs/PORT_JAVAFX_PLAN.md` for the reusable-vs-application boundary and package responsibilities.
- There is no root `README.md`; use the docs above as the entry point.

## Validation strategy

- Use the checked-in Gradle wrapper from the repository root.
- Prefer lightweight validation for small changes, such as `./gradlew --no-daemon compileJava`, `./gradlew --no-daemon testClasses`, or targeted JUnit tests.
- Run targeted tests with `./gradlew --no-daemon test --tests fully.qualified.TestClassName` and include multiple `--tests` filters when touching related classes.
- Use `./gradlew --no-daemon build` for large, risky, dependency, module, serialization, or cross-cutting changes.
- Documentation-only changes normally do not need Gradle validation unless they affect generated/compiled examples or documented commands.
- Do not launch manual JavaFX/Swing sample applications as routine validation; only run them when the task explicitly requires manual UI verification.
- Avoid running the full test suite during setup or routine small changes unless the task requires it.

## Manual and support applications

- Manual launchers are Gradle `JavaExec` tasks in `build.gradle`: `runTestScreen`, `runManagementApp`, `runScreenDesigner`, `runConversationEditor`, and `runCodeTableManager`.
- These applications are for human/manual checks and authoring diagnostics, not routine automated validation.
- Example files under `examples/user-manual` are reference snippets and are not compiled by the Gradle build unless a task explicitly wires them in.

## Coding guidance

- Prefer small, focused changes that preserve existing public APIs unless the task asks for an API change.
- Add or update tests near the touched feature package under `src/test/java/com/eb/javafx/...`.
- When adding a dependency, update `build.gradle`, the Java module declarations, and any setup/docs that depend on it.
- Keep JSON loaders, serializers, validation reports, and immutable update helpers deterministic and covered by focused tests.
- For public or user-facing features, update `docs/USER_MANUAL.md` and matching entries/examples under `examples/user-manual` when applicable.
- Reuse existing utility classes and patterns in `com.eb.javafx.util` before adding new helpers.

## Copilot setup and secrets

- `.github/workflows/copilot-setup-steps.yml` prepares Java 17 and warms Gradle dependencies with `compileJava testClasses`.
- The setup workflow uses the `copilot` environment and minimal `contents: read` permissions.
- The setup workflow expects an `ALTLIFE_TOKEN` environment secret only to configure Git access to the private AltLife source/application repository referenced by the setup workflow.
- Do not hard-code `ALTLIFE_TOKEN` or any other token.
- AltLife is a related private application repository used for setup-time access/context, while this repository should keep reusable engine code generic and free of application-specific content.
- If setup fails because `ALTLIFE_TOKEN` is missing, configure it as a masked GitHub environment secret under Settings > Environments > `copilot`, then re-run the setup workflow.
- Keep GitHub MCP tools enabled for repository context, releases, issues, pull requests, workflow runs, and CI log investigation.
- Never commit MCP tokens, Copilot tokens, GitHub tokens, or other credentials to source, workflow files, docs examples, logs, or change summaries.

## Change summary

- After completing any set of code changes, write a summary of those changes to `change_summary.md` in the repository root.
- If the current branch already has a `change_summary.md`, append the new summary to the end of the file.
- Before checking in, rename `change_summary.md` to `change_summary_<sanitized-branch-name>.md`.
- Replace non-alphanumeric branch-name separators such as forward slashes and hyphens with underscores.
- For example, `feature/add` becomes `change_summary_feature_add.md`; the current branch `copilot/add-copilot-instructions-file` uses `change_summary_copilot_add_copilot_instructions_file.md`.

## Onboarding notes and workarounds

- During this onboarding pass, attempting to read `/README.md` failed because the repository does not have a root README. The workaround is to start from `docs/USER_MANUAL.md`, `examples/user-manual/README.md`, and the Gradle files.
- The existing Copilot setup workflow already checks out the repository, configures Java 17, and warms Gradle dependencies, so no additional setup workflow change was needed for this instructions-only onboarding.
