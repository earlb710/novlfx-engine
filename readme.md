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

## Android Studio

- Open `/home/runner/work/novlfx-engine/novlfx-engine` as a Gradle project in Android Studio.
- Let Android Studio use the checked-in Gradle wrapper during sync.
- Use the shared **Build** and **Test** Gradle run configurations, or run the same tasks from the Gradle tool window.
- Keep `local.properties` local to your machine; Android Studio will create or update it with your SDK path as needed.
