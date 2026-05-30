package com.eb.javafx.util;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Thread-safe cache for rasterised SVG {@link Image} instances, keyed by an opaque host-
 * supplied string.  Hosts that want SVG → raster results memoised across the application
 * route this single helper instead of each call site standing up its own
 * {@link ConcurrentHashMap}.
 *
 * <h2>Why a shared cache</h2>
 *
 * <p>An SVG rasterisation pipeline (Batik) costs ~10–50 ms per image.  Application surfaces
 * that re-render frequently — inline stat icons rebuilt every UI tick, status-log rows
 * spawned on every state change, scene backgrounds applied on every route swap — should
 * never pay that cost on the hot path.  A single shared store also lets independent host
 * surfaces (header icons, sheet icons, etc.) reuse the same rasterised image when they ask
 * for the same {@code (resource, size)} pair, instead of standing up redundant per-call-site
 * caches that funnel into the same loader anyway.</p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * String key = SvgRasterCache.keyFor("com/example/stat-love.svg", 32, 32);
 * Image image = SvgRasterCache.getOrCompute(key, () ->
 *         myRasteriseFunction("com/example/stat-love.svg", 32, 32));
 * if (image != null) {
 *     // ... use the image
 * }
 * }</pre>
 *
 * <h2>Failure handling</h2>
 *
 * <p>A {@link #getOrCompute} call whose {@code loader} returns {@code null} writes a
 * sentinel image into the cache slot so subsequent lookups of the same key return
 * {@code null} immediately without re-invoking the loader.  Hosts that need a retry path
 * (e.g. after fixing an authoring error) can call {@link #invalidate}.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>All operations are thread-safe; the underlying store is a {@link ConcurrentHashMap}.
 * A background warm-up thread populating the cache concurrent with UI-thread reads is the
 * intended usage pattern.</p>
 */
public final class SvgRasterCache {

    /** Sentinel for cache slots whose loader returned null.  Distinguishes
     *  "never tried" (key absent) from "tried, failed" (key maps to FAILED). */
    private static final Image FAILED = new WritableImage(1, 1);

    private static final ConcurrentMap<String, Image> CACHE = new ConcurrentHashMap<>();

    private SvgRasterCache() {
    }

    /**
     * Returns the cached image for {@code key}, computing it via {@code loader} when this
     * is the first call for that key.  Subsequent lookups return the cached value (or
     * {@code null} when the loader previously returned {@code null}).
     *
     * @param key    opaque cache key — the caller chooses the convention.  Use
     *               {@link #keyFor(String, int, int)} for the standard
     *               {@code resourcePath@WxH} shape.
     * @param loader rasterisation function invoked at most once per key.  Returning
     *               {@code null} is treated as a permanent failure for that key (see
     *               class-level Javadoc).
     * @return the cached image, or {@code null} when the loader returned {@code null}.
     */
    public static Image getOrCompute(String key, Supplier<Image> loader) {
        if (key == null) {
            return null;
        }
        if (loader == null) {
            throw new IllegalArgumentException("Loader is required.");
        }
        Image cached = CACHE.computeIfAbsent(key, k -> {
            Image produced;
            try {
                produced = loader.get();
            } catch (RuntimeException ex) {
                System.err.println("[SvgRasterCache] Loader threw for key '" + k + "': " + ex);
                ex.printStackTrace();
                produced = null;
            }
            return produced == null ? FAILED : produced;
        });
        return cached == FAILED ? null : cached;
    }

    /**
     * Builds the standard cache key for a resource path rasterised at a specific width
     * and height.  Format: {@code resourcePath + "@" + width + "x" + height} — chosen so
     * the same SVG rasterised at multiple sizes (status-log icon at 32×32 vs header icon
     * at 28×28) lives in distinct cache slots.
     */
    public static String keyFor(String resourcePath, int width, int height) {
        return resourcePath + "@" + width + "x" + height;
    }

    /** Drops a single key from the cache so the next {@link #getOrCompute} re-invokes
     *  its loader.  Useful for hot-reload flows. */
    public static void invalidate(String key) {
        if (key != null) {
            CACHE.remove(key);
        }
    }

    /** Empties the cache entirely — typically only used by tests / dev tooling. */
    public static void clear() {
        CACHE.clear();
    }

    /** Returns the current number of cached entries (including failure sentinels).
     *  Exposed for diagnostics / startup-warm-up logging. */
    public static int size() {
        return CACHE.size();
    }
}
