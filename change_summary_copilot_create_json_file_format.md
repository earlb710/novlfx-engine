Added JSON-backed map and location text support for gamesupport:

- Added `MapTextDefinition` and `MapTextEntry` for localized map text files with `language`, `maps`, `mapId`, and optional `description` defaulting to `Main Map`.
- Added `LocationTextDefinition`, `LocationTextEntry`, and `LocationDescriptionVariant` for localized per-map location text files with `language`, `mapId`, `locations`, `locId`, description variants, optional conditions, and `mapId.locId` reference helpers.
- Added focused tests and test resources for parsing, defaults, duplicate validation, condition matching, references, and JSON round trips.
- Documented both JSON formats in the user manual and added matching user-manual example JSON files.
- Validated with `./gradlew --no-daemon compileJava test --tests com.eb.javafx.gamesupport.MapAndLocationTextDefinitionTest --tests com.eb.javafx.gamesupport.LocationSupportTest`.

Added Default App Values management support for editing the bundled map/location JSON examples:

- Added a new **Locations** tab next to **Application Values** in `DefaultDisplayValuesApplication`.
- Embedded map-text and location-text JSON editors with **Save**, **Format**, and **Reset** actions backed by `MapTextDefinition` and `LocationTextDefinition`.
- Loaded the editor content from the user-manual example JSON files with deterministic fallbacks for tests.
- Added focused UI/helper tests for the new tab labels, sample JSON loading, and editor actions.
- Updated the user manual to describe the new Locations tab workflow.
- Validated with `./gradlew --no-daemon compileJava test --tests com.eb.javafx.testscreen.DefaultDisplayValuesApplicationTest --tests com.eb.javafx.testscreen.ManagementApplicationTest`.

Refreshed the user manual for the new map/location JSON editing flow:

- Updated the management-app overview to call out the Default App Values **Locations** tab.
- Expanded the `MapTextDefinition` and `LocationTextDefinition` sections with load/save/toJson usage guidance.
- Added the bundled `map-text.demo.json` and `location-text-town.demo.json` files to the manual’s example list for section 9.
