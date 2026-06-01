package com.eb.javafx.bootstrap;

import com.eb.javafx.util.FontResources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the config-driven font registration modding hook — {@code font.*} entries in the app
 * config {@code resources} map are registered at boot from the classpath or from disk.
 */
final class ConfiguredFontsTest {

    private static ApplicationResourceConfig configWith(Map<String, String> resources) {
        return ApplicationResourceConfig.of(resources);
    }

    @Test
    void registersOnlyFontPrefixedEntriesFromClasspath() {
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("uiTheme", "some/theme.css");                       // ignored: not font.*
        resources.put("altlife.locationContent", "loc.json");            // ignored: not font.*
        resources.put("font.alien", "classpath:/com/eb/javafx/fonts/Alien.ttf");
        resources.put("font.crimson", "classpath:com/eb/javafx/fonts/Crimson-Bold.ttf"); // no leading slash

        int registered = ConfiguredFonts.register(configWith(resources), null);

        assertEquals(2, registered, "Only the two font.* entries should be registered.");
    }

    @Test
    void registersFontFromFileUnderApplicationRoot(@TempDir Path applicationRoot) throws Exception {
        Path fontFile = applicationRoot.resolve("mods/fonts/Modded.ttf");
        Files.createDirectories(fontFile.getParent());
        try (InputStream inputStream = FontResources.open("Alien.ttf")) {
            Files.copy(inputStream, fontFile);
        }

        // Relative path is resolved against the application root and loaded from disk.
        int registered = ConfiguredFonts.register(
                configWith(Map.of("font.body", "mods/fonts/Modded.ttf")), applicationRoot);

        assertEquals(1, registered);
    }

    @Test
    void skipsUnresolvableFontWithoutFailing() {
        int registered = ConfiguredFonts.register(
                configWith(Map.of("font.missing", "classpath:/com/eb/javafx/fonts/NoSuchFont.ttf")), null);

        assertEquals(0, registered, "A bad font entry is logged and skipped, never aborting boot.");
    }

    @Test
    void countsValidEntriesAndSkipsBadOnesTogether() {
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("font.good", "classpath:/com/eb/javafx/fonts/Alien.ttf");
        resources.put("font.bad", "classpath:/com/eb/javafx/fonts/Absent.ttf");

        assertEquals(1, ConfiguredFonts.register(configWith(resources), null));
    }

    @Test
    void noFontEntriesRegistersNothing() {
        assertEquals(0, ConfiguredFonts.register(
                configWith(Map.of("uiTheme", "theme.css")), null));
    }

    @Test
    void nullConfigIsSafe() {
        assertEquals(0, ConfiguredFonts.register(null, null));
    }
}
