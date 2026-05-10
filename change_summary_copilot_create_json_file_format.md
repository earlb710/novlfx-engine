Added JSON-backed map and location text support for gamesupport:

- Added `MapTextDefinition` and `MapTextEntry` for localized map text files with `language`, `maps`, `mapId`, and optional `description` defaulting to `Main Map`.
- Added `LocationTextDefinition`, `LocationTextEntry`, and `LocationDescriptionVariant` for localized per-map location text files with `language`, `mapId`, `locations`, `locId`, description variants, optional conditions, and `mapId.locId` reference helpers.
- Added focused tests and test resources for parsing, defaults, duplicate validation, condition matching, references, and JSON round trips.
- Documented both JSON formats in the user manual and added matching user-manual example JSON files.
- Validated with `./gradlew --no-daemon compileJava test --tests com.eb.javafx.gamesupport.MapAndLocationTextDefinitionTest --tests com.eb.javafx.gamesupport.LocationSupportTest`.
