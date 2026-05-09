Added a second block background image test class that mirrors the existing manual/renderer assertions while loading screen and block background metadata from a JSON screen definition.
Added a JSON fixture under src/test/resources with screen-level and block-level background image metadata, text rows, and button rows for the new test coverage.
Validated the related block background tests with ./gradlew --no-daemon compileJava test --tests com.eb.javafx.ui.test.BlockBackgroundImageTestScreenTest --tests com.eb.javafx.ui.test.JsonBlockBackgroundImageTestScreenTest.
