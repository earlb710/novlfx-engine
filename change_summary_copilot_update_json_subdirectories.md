Updated the example JSON directory layout under `examples/resources/json` to use the new type-specific subdirectories.

- Moved the startup load definition from `examples/resources/json/app-load/app-load.json` to `examples/resources/json/config/app-load.json` and updated bootstrap defaults/tests to look in `config/app-load.json`.
- Consolidated the map/location example files under `examples/resources/json/location/`.
- Updated the user manual, example README, and manual-support UI/test strings to document and display the new paths.
- Validated the change with `./gradlew --no-daemon test --tests com.eb.javafx.bootstrap.BootstrapServiceTest --tests com.eb.javafx.testscreen.DefaultDisplayValuesApplicationTest`.
