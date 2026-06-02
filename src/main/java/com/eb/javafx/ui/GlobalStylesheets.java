package com.eb.javafx.ui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Process-wide ordered set of additional stylesheet URLs applied to every themed scene, layered
 * after the engine theme and footer stylesheets.
 *
 * <p>This is the CSS analogue of {@link javafx.scene.text.Font#loadFont} for fonts: an extension
 * module (discovered via the engine SPI) contributes a stylesheet once, and every scene built
 * through {@link com.eb.javafx.routing.RouteContext} picks it up. URLs must be produced by the
 * contributing module's own {@code getResource(...)} so they carry that module's class loader and
 * load without JPMS encapsulation issues.</p>
 */
public final class GlobalStylesheets {

    private static final List<String> URLS = new CopyOnWriteArrayList<>();

    private GlobalStylesheets() {
    }

    /** Registers a stylesheet URL (idempotent; blank / duplicate URLs are ignored). */
    public static void add(String url) {
        if (url != null && !url.isBlank() && !URLS.contains(url)) {
            URLS.add(url);
        }
    }

    /** The registered stylesheet URLs, in registration order. */
    public static List<String> all() {
        return List.copyOf(URLS);
    }

    /** Removes all registered stylesheets (used by tests). */
    public static void clear() {
        URLS.clear();
    }
}
