# novlfx-engine

Reusable JavaFX foundation extracted from Lr2Alt.

This initial snapshot intentionally preserves the existing `com.lr2alt.javafx.*` package names while publishing them from the `com.novlfx.engine` Java module. Rename packages later after Lr2Alt consumes the library dependency.

## Included

- Reusable JavaFX foundation packages:
  - `audio`
  - `bootstrap`
  - `content`
  - `display`
  - `gamesupport`
  - `prefs`
  - `random`
  - `renpy`
  - `routing`
  - `save`
  - `state`
  - `text`
  - `ui`
- `src/main/resources/com/lr2alt/javafx/ui/lr2alt.css`
- Matching JUnit tests for the reusable packages
- Test-only `testscreen` support used by the copied UI tests

## Excluded

- Lr2Alt application entry point: `GameApplication`
- Authored Lr2Alt game content: `gamecontent/display`
- Ren'Py game scripts and assets from Lr2Alt's `game` directory
- Extraction metadata classes from Lr2Alt's `engine` package
- Lr2Alt-specific migration docs

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
