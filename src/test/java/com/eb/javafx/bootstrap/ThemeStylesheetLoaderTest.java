package com.eb.javafx.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ThemeStylesheetLoaderTest {

    @Test
    void resolvesExistingOverrideFileToFileUrl(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("theme.css"), ".x { -fx-font-size: 10px; }");
        ApplicationResourceConfig config = ApplicationResourceConfig.of(Map.of("uiTheme", "theme.css"));

        Optional<String> url = ThemeStylesheetLoader.resolveOverrideUrl(config, root, "uiTheme");

        assertTrue(url.isPresent());
        assertTrue(url.get().startsWith("file:"));
        assertTrue(url.get().endsWith("theme.css"));
    }

    @Test
    void emptyWhenOverridePointsAtMissingFile(@TempDir Path root) {
        ApplicationResourceConfig config = ApplicationResourceConfig.of(Map.of("uiTheme", "absent.css"));
        assertFalse(ThemeStylesheetLoader.resolveOverrideUrl(config, root, "uiTheme").isPresent());
    }

    @Test
    void emptyWhenResourceIdUnsetOrConfigNull(@TempDir Path root) {
        assertFalse(ThemeStylesheetLoader.resolveOverrideUrl(
                ApplicationResourceConfig.of(Map.of()), root, "uiTheme").isPresent());
        assertFalse(ThemeStylesheetLoader.resolveOverrideUrl(null, root, "uiTheme").isPresent());
    }
}
