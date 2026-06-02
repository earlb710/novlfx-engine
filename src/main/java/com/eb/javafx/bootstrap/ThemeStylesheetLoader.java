package com.eb.javafx.bootstrap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves a config-supplied theme-stylesheet override to a loadable {@code file:} URL.
 *
 * <p>Generic modding plumbing: a game points a {@code resources} id (conventionally {@code uiTheme})
 * at an external CSS file to restyle the UI without a rebuild. This resolves that id against the
 * application root and only accepts it when it points at a readable file on disk; otherwise the
 * bundled stylesheet stays in effect. Side-effect-free, so it is unit-testable without a live
 * boot context.</p>
 */
public final class ThemeStylesheetLoader {

    private ThemeStylesheetLoader() {
    }

    /**
     * Resolves {@code resourceId} (e.g. {@code "uiTheme"}) from {@code config} to a {@code file:} URL
     * when it points at a regular file under {@code applicationRoot}; empty otherwise (use bundled
     * CSS). Resolution failure is swallowed and returns empty — the override is best-effort.
     */
    public static Optional<String> resolveOverrideUrl(ApplicationResourceConfig config,
            Path applicationRoot, String resourceId) {
        if (config == null) {
            return Optional.empty();
        }
        try {
            return config.resolveResource(applicationRoot, resourceId)
                    .filter(Files::isRegularFile)
                    .map(path -> path.toUri().toString());
        } catch (RuntimeException exception) {
            System.err.println("[ThemeStylesheetLoader] Could not resolve theme stylesheet '"
                    + resourceId + "': " + exception);
            return Optional.empty();
        }
    }
}
