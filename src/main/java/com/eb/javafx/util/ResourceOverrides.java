package com.eb.javafx.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Config-driven asset override root — lets a game / mod replace bundled icons & images purely
 * through setup, no rebuild.
 *
 * <p>When an override root directory is set (from the {@code resources.assetOverrideRoot} entry
 * in {@code config.json} at boot), any resource loader that consults this class will prefer a
 * file at <code>&lt;overrideRoot&gt;/&lt;resourcePath&gt;</code> over the classpath copy.  The
 * override directory simply mirrors the bundled resource path:</p>
 *
 * <pre>
 * config.json:  "resources": { "assetOverrideRoot": "mods/assets" }
 *
 * To replace the energy stat icon, drop a file at:
 *   mods/assets/com/altlife/javafx/images/stats/stat-energy-positive.svg
 * To replace the save footer icon:
 *   mods/assets/&lt;footer-icon-dir&gt;/footer-save.svg
 * </pre>
 *
 * <p>Loaders fall back to the bundled classpath resource when no override file exists, so a
 * partial override directory only replaces the files it actually contains.  Lookups are guarded
 * against path-escape (a {@code ../} spec that climbs out of the root is ignored).</p>
 */
public final class ResourceOverrides {

    /** Reserved {@code resources} id naming the override directory in {@code config.json}. */
    public static final String OVERRIDE_ROOT_RESOURCE_ID = "assetOverrideRoot";

    /** Image extensions an icon/image override may use — so a mod can replace an {@code .svg}
     *  icon with a {@code .png} (or any of these) by name, not just same-extension. */
    private static final java.util.List<String> IMAGE_EXTENSIONS =
            java.util.List.of(".svg", ".png", ".jpg", ".jpeg", ".gif", ".bmp");

    private static volatile Path overrideRoot;

    /** Per-resource repoint map: normalised original resource path → replacement resource path.
     *  Populated from {@code resources} entries keyed {@code icon:<originalPath>} (see
     *  {@link #ALIAS_RESOURCE_PREFIX}).  Lets a mod replace a single icon without mirroring its
     *  full path under the override root.  The replacement is itself resolved override-root-first
     *  then classpath, so it may live in the override folder or be a bundled resource. */
    private static volatile java.util.Map<String, String> aliases = java.util.Map.of();

    /** {@code resources} id prefix marking a per-resource icon repoint (key minus the prefix is
     *  the original resource path; value is the replacement resource path). */
    public static final String ALIAS_RESOURCE_PREFIX = "icon:";

    private ResourceOverrides() {
    }

    /** Sets the override root directory (or clears it when {@code null} / not a directory). */
    public static void setOverrideRoot(Path directory) {
        overrideRoot = (directory != null && Files.isDirectory(directory))
                ? directory.toAbsolutePath().normalize()
                : null;
    }

    /** Installs the per-resource repoint map (keys/values are resource-style paths).  Keys are
     *  normalised (backslashes → '/', leading slash stripped).  Pass null/empty to clear. */
    public static void setAliases(java.util.Map<String, String> originalToReplacement) {
        if (originalToReplacement == null || originalToReplacement.isEmpty()) {
            aliases = java.util.Map.of();
            return;
        }
        java.util.LinkedHashMap<String, String> normalised = new java.util.LinkedHashMap<>();
        originalToReplacement.forEach((key, value) -> {
            if (key != null && value != null && !value.isBlank()) {
                normalised.put(normalise(key), value.trim());
            }
        });
        aliases = java.util.Map.copyOf(normalised);
    }

    private static String normalise(String path) {
        String forward = path.replace('\\', '/');
        while (forward.startsWith("/")) {
            forward = forward.substring(1);
        }
        return forward;
    }

    /** Returns the alias replacement for {@code resourcePath}, or null when none is registered. */
    private static String aliasFor(String resourcePath) {
        return resourcePath == null ? null : aliases.get(normalise(resourcePath));
    }

    /** Returns the per-resource alias replacement for {@code resourcePath}, or the path itself
     *  when no alias is registered.  Use at classpath-fallback sites so an alias is honored even
     *  when its replacement is a bundled resource rather than an override-root file. */
    public static String effectivePath(String resourcePath) {
        String alias = aliasFor(resourcePath);
        return alias != null ? alias : resourcePath;
    }

    /** Returns the active override root, if any (mainly for diagnostics / tests). */
    public static Optional<Path> overrideRoot() {
        return Optional.ofNullable(overrideRoot);
    }

    /**
     * Returns the override file URL for {@code resourcePath} when an override root is set and a
     * matching file exists; empty otherwise.  {@code resourcePath} is a classpath-style path
     * (leading slash optional), e.g. {@code com/altlife/javafx/images/stats/x.svg}.
     */
    public static Optional<URL> find(String resourcePath) {
        Path root = overrideRoot;
        if (root == null || resourcePath == null || resourcePath.isBlank()) {
            return Optional.empty();
        }
        // A per-resource alias repoints to a different resource path before the root lookup.
        String alias = aliasFor(resourcePath);
        String relative = normalise(alias != null ? alias : resourcePath);
        Path candidate = root.resolve(relative).normalize();
        // Guard against a path that climbs out of the override root via ../ segments.
        if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)) {
            return Optional.empty();
        }
        try {
            return Optional.of(candidate.toUri().toURL());
        } catch (MalformedURLException exception) {
            return Optional.empty();
        }
    }

    /**
     * Like {@link #find} but for icons/images: returns an override matching the exact path first,
     * then the same base name with any supported image extension (so a {@code .png} can override
     * an {@code .svg}).  Empty when none exists.
     */
    public static Optional<URL> findImage(String resourcePath) {
        return resolveImagePath(resourcePath).flatMap(ResourceOverrides::find);
    }

    /**
     * Returns the <em>resource path</em> (in the same classpath-style form as the input) of the
     * override that should be used for an icon/image: the exact path when present, else the same
     * base with another supported image extension.  Empty when no override file exists.
     *
     * <p>Callers feed the returned path back into their normal (override-aware) loader, so an
     * SVG override stays on the cached SVG path and a raster override switches the loader to the
     * matching bitmap decoder — all from one resolution step.</p>
     */
    public static Optional<String> resolveImagePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return Optional.empty();
        }
        // A per-resource alias repoints to a replacement path; the caller re-opens it (which
        // resolves override-root-first, then classpath), so classpath replacements work too.
        String alias = aliasFor(resourcePath);
        if (alias != null) {
            return Optional.of(alias);
        }
        if (overrideRoot == null) {
            return Optional.empty();
        }
        if (find(resourcePath).isPresent()) {
            return Optional.of(resourcePath);
        }
        String base = stripExtension(resourcePath);
        for (String extension : IMAGE_EXTENSIONS) {
            String candidate = base + extension;
            if (!candidate.equals(resourcePath) && find(candidate).isPresent()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static String stripExtension(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        int dot = path.lastIndexOf('.');
        return dot > slash ? path.substring(0, dot) : path;
    }

    /**
     * Opens an override stream for {@code resourcePath}, or {@code null} when no override exists
     * (caller should then fall back to the classpath).  Callers must close the returned stream.
     */
    public static InputStream open(String resourcePath) throws IOException {
        Optional<URL> url = find(resourcePath);
        if (url.isEmpty()) {
            return null;
        }
        return url.get().openStream();
    }
}
