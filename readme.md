# novlfx-engine

Reusable JavaFX foundation for visual novel games.

The library publishes reusable packages in the `com.eb.javafx.*` namespace through the `com.novlfx.engine` Java module.

## User manual

See [docs/USER_MANUAL.md](docs/USER_MANUAL.md) for setup, package responsibilities, service usage, and extension boundaries.

## Included

- Reusable JavaFX foundation packages:
  - `audio`
  - `bootstrap`
  - `content`
  - `display`
  - `gamesupport`
  - `prefs`
  - `random`
  - `globalApi`
  - `routing`
  - `save`
  - `scene`
  - `state`
  - `text`
  - `ui`
  - `util`
- `src/main/resources/com/eb/javafx/ui/eb.css`
- Matching JUnit tests for the reusable packages
- Test-only `testscreen` support used by the copied UI tests

## Excluded

- eb application entry point: `GameApplication`
- Authored eb game content: `gamecontent/display`
- Source game scripts and assets from eb's `game` directory
- Extraction metadata classes from eb's `engine` package
- eb-specific migration docs

## Validate

```bash
./gradlew --no-daemon build
```

## Launch the test screen

```bash
./gradlew --no-daemon runTestScreen
```

## Android Studio

- Open the repository root as a Gradle project in Android Studio.
- Let Android Studio use the checked-in Gradle wrapper during sync.
- Run Gradle tasks such as `build`, `test`, and `runTestScreen` from the Gradle tool window.
- Import the shared `Test Screen` run configuration from `.run/Test Screen.run.xml` if Android Studio does not pick it up automatically after sync.
- Keep `local.properties` local to your machine; Android Studio will create or update it with your SDK path as needed.
