package com.eb.javafx.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Produces a font-scaled copy of a CSS stylesheet as a cached temp file, returning a {@code file:}
 * URI a JavaFX {@link javafx.scene.Scene} can load.
 *
 * <p>Generic accessibility plumbing: the global "Text size" scale must grow / shrink every
 * px-pinned {@code -fx-font-size} in a stylesheet (a root font-size alone won't), so the sheet is
 * rewritten via {@link FontScaling#scale}. The result is cached per {@code (sourceUrl, scale)} so a
 * scene re-attach doesn't re-encode. When the scale is effectively 1.0 the original URL is returned
 * unchanged (no temp file).</p>
 */
public final class ScaledStylesheet {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private ScaledStylesheet() {
    }

    /**
     * Returns a {@code file:} URI for {@code sourceUrl} scaled by {@code scale}, or {@code sourceUrl}
     * unchanged when {@code scale ≈ 1.0} or {@code sourceUrl} is null. On any read/write failure the
     * original URL is returned (scaling is best-effort — the unscaled sheet always works).
     */
    public static String scaledUri(String sourceUrl, double scale) {
        if (sourceUrl == null) {
            return null;
        }
        if (Math.abs(scale - 1.0) < 1.0e-6) {
            return sourceUrl;
        }
        String key = sourceUrl + "|" + scale;
        String cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try (InputStream in = new URL(sourceUrl).openStream()) {
            String css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String scaled = FontScaling.scale(css, scale);
            Path temp = Files.createTempFile("novlfx-theme-scaled-", ".css");
            temp.toFile().deleteOnExit();
            Files.writeString(temp, scaled, StandardCharsets.UTF_8);
            String uri = temp.toUri().toString();
            CACHE.put(key, uri);
            return uri;
        } catch (IOException exception) {
            System.err.println("[ScaledStylesheet] Failed to scale " + sourceUrl
                    + " by " + scale + ": " + exception);
            return sourceUrl;
        }
    }
}
