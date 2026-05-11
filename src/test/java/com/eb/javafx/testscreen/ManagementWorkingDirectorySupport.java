package com.eb.javafx.testscreen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.prefs.Preferences;

/** Shared working-directory helpers for manual management screens. */
final class ManagementWorkingDirectorySupport {
    private static final String WORKING_DIRECTORY_KEY = "workingDirectory";

    private ManagementWorkingDirectorySupport() {
    }

    static Preferences preferences() {
        return Preferences.userNodeForPackage(ManagementApplication.class);
    }

    static Path load() {
        return load(preferences());
    }

    static Path load(Preferences preferences) {
        String stored = preferences.get(WORKING_DIRECTORY_KEY, "");
        if (!stored.isBlank()) {
            try {
                Path path = Path.of(stored).toAbsolutePath().normalize();
                if (Files.isDirectory(path)) {
                    return path;
                }
            } catch (RuntimeException ignored) {
                // Fall through to default.
            }
        }
        return defaultWorkingDirectory();
    }

    static void save(Path workingDirectory) {
        save(preferences(), workingDirectory);
    }

    static void save(Preferences preferences, Path workingDirectory) {
        preferences.put(WORKING_DIRECTORY_KEY, normalizeDirectory(workingDirectory).toString());
    }

    static Path defaultWorkingDirectory() {
        return Path.of("").toAbsolutePath().normalize();
    }

    static Path initialDirectory(Path workingDirectory, Path fallbackDirectory) {
        if (workingDirectory != null) {
            Path normalizedWorkingDirectory = workingDirectory.toAbsolutePath().normalize();
            if (Files.isDirectory(normalizedWorkingDirectory)) {
                return normalizedWorkingDirectory;
            }
        }
        return normalizeDirectory(fallbackDirectory);
    }

    static Path normalizeDirectory(Path directory) {
        Path normalized = Objects.requireNonNull(directory, "Working directory is required.")
                .toAbsolutePath()
                .normalize();
        if (!Files.isDirectory(normalized)) {
            throw new IllegalArgumentException("Working directory must be a directory: " + normalized);
        }
        return normalized;
    }

    static Path chooserStartDirectory(String currentValue, Path workingDirectory) {
        if (currentValue != null && !currentValue.isBlank()) {
            try {
                Path path = Path.of(currentValue);
                Path normalized = path.isAbsolute()
                        ? path.toAbsolutePath().normalize()
                        : normalizeDirectory(workingDirectory).resolve(path).normalize();
                Path chooserPath = Files.isDirectory(normalized)
                        ? normalized
                        : normalized.getParent();
                if (chooserPath != null && Files.exists(chooserPath)) {
                    return chooserPath;
                }
            } catch (RuntimeException ignored) {
                // Fall through to working directory.
            }
        }
        return normalizeDirectory(workingDirectory);
    }
}
