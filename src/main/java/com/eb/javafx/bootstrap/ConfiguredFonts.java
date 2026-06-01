package com.eb.javafx.bootstrap;

import com.eb.javafx.util.FontResources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Registers extra fonts declared in the application config so a game / mod can add fonts
 * purely through setup — no Java change, no whitelist edit.
 *
 * <p>Any entry in the config {@code resources} map whose id starts with {@value #FONT_RESOURCE_PREFIX}
 * is treated as a font to register at boot.  The value is the font's location, resolved in this
 * order:</p>
 * <ol>
 *   <li>{@code classpath:<path>} — loaded from the classpath (bundled with the game/mod jar).</li>
 *   <li>a path that resolves to an existing file under the application root — loaded from disk
 *       (lets a modder drop a real {@code .ttf}/{@code .otf} next to {@code config.json}).</li>
 *   <li>otherwise — tried as a bare classpath resource path.</li>
 * </ol>
 *
 * <p>Example {@code config.json}:</p>
 * <pre>
 * "resources": {
 *   "font.title":  "mods/fonts/MyTitleFont.ttf",
 *   "font.body":   "classpath:/com/mygame/fonts/Body.otf"
 * }
 * </pre>
 *
 * <p>Once registered, a font's <b>family name</b> (not the filename) is usable in CSS via
 * {@code -fx-font-family}.  Loading is best-effort: a bad path logs and is skipped so one
 * missing mod font can't abort boot.</p>
 */
public final class ConfiguredFonts {

    /** Resource-id prefix that marks a {@code resources} entry as a font to register. */
    public static final String FONT_RESOURCE_PREFIX = "font.";

    private static final double REGISTRATION_SIZE = 12.0;
    private static final String CLASSPATH_SCHEME = "classpath:";

    private ConfiguredFonts() {
    }

    /**
     * Registers every {@code font.*} entry in the config.  Must be called on the JavaFX
     * application thread (JavaFX {@code Font.loadFont} requires the toolkit to be up).
     *
     * @return the number of fonts successfully registered
     */
    public static int register(ApplicationResourceConfig config, Path applicationRoot) {
        if (config == null) {
            return 0;
        }
        int registered = 0;
        for (Map.Entry<String, String> entry : config.resources().entrySet()) {
            if (!entry.getKey().startsWith(FONT_RESOURCE_PREFIX)) {
                continue;
            }
            try {
                if (loadOne(entry.getValue(), applicationRoot)) {
                    registered++;
                }
            } catch (RuntimeException exception) {
                System.err.println("[ConfiguredFonts] Failed to register font '" + entry.getKey()
                        + "' = " + entry.getValue() + ": " + exception);
            }
        }
        return registered;
    }

    private static boolean loadOne(String spec, Path applicationRoot) {
        if (spec == null || spec.isBlank()) {
            return false;
        }
        String trimmed = spec.trim();
        if (trimmed.startsWith(CLASSPATH_SCHEME)) {
            FontResources.loadResource(stripClasspath(trimmed), REGISTRATION_SIZE);
            return true;
        }
        if (applicationRoot != null) {
            Path file = applicationRoot.resolve(trimmed);
            if (Files.isRegularFile(file)) {
                FontResources.loadFile(file, REGISTRATION_SIZE);
                return true;
            }
        }
        // Not an on-disk file — fall back to treating the spec as a classpath resource.
        FontResources.loadResource(trimmed, REGISTRATION_SIZE);
        return true;
    }

    private static String stripClasspath(String spec) {
        String path = spec.substring(CLASSPATH_SCHEME.length());
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
