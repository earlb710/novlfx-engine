package com.eb.javafx.util;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Path helpers for authored assets and repository-relative resources.
 *
 * <p>The helpers normalize separators, safely resolve child paths without allowing root escapes, derive
 * normalized file names or extensions for asset lookup code, and centralize common absolute path handling.</p>
 */
public final class PathUtils {
    private PathUtils() {
    }

    public static String normalizeSeparators(String path) {
        return path == null ? null : path.replace('\\', '/');
    }

    public static Path absoluteNormalized(Path path) {
        return Validation.requireNonNull(path, "Path is required.").toAbsolutePath().normalize();
    }

    public static Path currentDirectory() {
        return absoluteNormalized(Path.of(""));
    }

    public static Path currentDirectory(String first, String... more) {
        return absoluteNormalized(Path.of(
                Validation.requireNonBlank(first, "Path is required."),
                more == null ? new String[0] : more));
    }

    public static Path temporaryPath(String first, String... more) {
        Path temporaryRoot = absoluteNormalized(Path.of(Validation.requireNonBlank(
                System.getProperty("java.io.tmpdir"),
                "Temporary directory property is required.")));
        Path child = Path.of(
                Validation.requireNonBlank(first, "Temporary path is required."),
                more == null ? new String[0] : more);
        return resolveChild(temporaryRoot, child.toString());
    }

    public static Path resolveChild(Path root, String childPath) {
        Validation.requireNonNull(root, "Root path is required.");
        String normalizedChild = Validation.requireNonBlank(childPath, "Child path is required.");
        Path normalizedRoot = root.normalize();
        Path resolved = normalizedRoot.resolve(normalizeSeparators(normalizedChild)).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Child path escapes root: " + childPath);
        }
        return resolved;
    }

    public static String fileName(String path) {
        String normalized = Validation.requireNonBlank(normalizeSeparators(path), "Path is required.");
        return Path.of(normalized).getFileName().toString();
    }

    public static String fileNameLowercase(String path) {
        return fileName(path).toLowerCase(Locale.ROOT);
    }

    public static String extensionLowercase(Path path) {
        Validation.requireNonNull(path, "Path is required.");
        String name = path.getFileName().toString();
        int separator = name.lastIndexOf('.');
        return separator < 0 ? "" : name.substring(separator).toLowerCase(Locale.ROOT);
    }
}
