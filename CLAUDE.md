# Claude guidance for novlfx-engine

## Repository overview

- Java 17 Gradle `java-library` project for a reusable JavaFX visual-novel/game engine.
- Java module: `com.novlfx.engine` — update `src/main/java/module-info.java` when adding exported packages or new module dependencies.
- Engine code: `src/main/java/com/eb/javafx/*`; tests and manual support apps: `src/test/java/com/eb/javafx/*`.
- Public APIs are organized by feature package: `bootstrap`, `routing`, `scene`, `ui`, `display`, `audio`, `save`, `prefs`, `gamesupport`, `text`, `util`, and others listed in `docs/USER_MANUAL.md`.
- Keep this repository generic and reusable — no game-specific story content, route names, assets, save schemas, or one-off migration helpers in engine code.
- App-authored example JSON lives under `examples/resources/json` by type: `code-tables`, `config`, `conversations`, `display`, `location`, `scenes`, and `screens`. Screen text sidecars (`*_text.json`) live next to their screen JSON files in `screens`.

## First files to read

- `build.gradle` and `settings.gradle` — Gradle toolchain, dependencies, test setup, and manual app tasks.
- `docs/USER_MANUAL.md` — user-facing API guide, package summary, validation commands, and manual tool descriptions.
- `examples/user-manual/README.md` — runnable/reference snippets mirroring the user manual sections.
- `docs/PORT_JAVAFX_PLAN.md` — reusable-vs-application boundary and package responsibilities.
- There is no root `README.md`; use the docs above as the entry point.

## Validation strategy

Use the checked-in Gradle wrapper from the repository root.

- **Small changes** (single class, minor logic): `./gradlew --no-daemon compileJava` or `./gradlew --no-daemon testClasses`
- **Targeted tests**: `./gradlew --no-daemon test --tests fully.qualified.TestClassName` — add multiple `--tests` filters when touching related classes
- **Large/risky changes** (dependencies, module changes, serialization, cross-cutting): `./gradlew --no-daemon build`
- **Docs-only changes**: no Gradle validation needed unless they affect compiled examples or documented commands
- Do not launch manual JavaFX/Swing apps as routine validation; only run them when the task explicitly requires manual UI verification.
- Do not run the full test suite for setup or routine small changes unless the task requires it.

## Manual and support applications

Manual launchers are Gradle `JavaExec` tasks in `build.gradle`:

| Task | Purpose |
|------|---------|
| `runTestScreen` | Interactive test screen |
| `runManagementApp` | Button-only launcher for authoring/diagnostic screens |
| `runScreenDesigner` | Screen designer (starts in `examples/screen-designs`) |
| `runConversationEditor` | Conversation editor (starts in `examples/conversations`) |
| `runCodeTableManager` | Code table manager |

These are for human/manual checks and authoring diagnostics, not automated validation.

Example code under `examples/user-manual` is reference material and is not compiled by the Gradle build unless explicitly wired in. JSON data examples live under `examples/resources/json`.

## Coding guidance

- Prefer small, focused changes that preserve existing public APIs unless the task asks for an API change.
- Add or update tests near the touched feature package under `src/test/java/com/eb/javafx/...`.
- When adding a dependency, update `build.gradle`, the Java module declarations, and any setup/docs that depend on it.
- Keep JSON loaders, serializers, validation reports, and immutable update helpers deterministic and covered by focused tests.
- For public or user-facing features, update `docs/USER_MANUAL.md` and matching entries/examples under `examples/user-manual` when applicable.
- Reuse existing utility classes and patterns in `com.eb.javafx.util` before adding new helpers.
- Do not hardcode runtime UI text in Java — externalize it into `*_text.json` sidecar files and load via `ScreenTextResources`.

## Change summary

- After completing any set of code changes, write a summary to `change_summary.md` in the repository root.
- If a `change_summary.md` already exists on the branch, append the new summary to the end.
- Before checking in, rename it to `change_summary_<sanitized-branch-name>.md`.
- Replace forward slashes, hyphens, and other non-alphanumeric characters with underscores.
  - Example: branch `feature/add-foo` → `change_summary_feature_add_foo.md`

## Onboarding

Start from `docs/USER_MANUAL.md`, `examples/user-manual/README.md`, and the Gradle files.
