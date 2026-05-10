package com.eb.javafx.bootstrap;

import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** One app-load entry that points at a JSON file or a directory of JSON files below the JSON resource root. */
public record ApplicationJsonLoad(ApplicationJsonLoadType type, String path, String fileName) {
    public ApplicationJsonLoad {
        type = Validation.requireNonNull(type, "Application JSON load type is required.");
        path = Validation.requireNonBlank(path, "Application JSON load path is required.");
        fileName = fileName == null ? "" : fileName;
    }

    public List<Path> resolvePaths(Path jsonResourceRoot) {
        Path directory = Validation.requireNonNull(jsonResourceRoot, "JSON resource root is required.")
                .resolve(path)
                .normalize();
        if (!fileName.isBlank()) {
            return List.of(directory.resolve(fileName).normalize());
        }
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Application JSON load directory does not exist: " + directory);
        }
        try (var paths = Files.list(directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(ApplicationJsonLoad::isJsonFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(Path::normalize)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to list application JSON load directory: " + directory, exception);
        }
    }

    private static boolean isJsonFile(Path path) {
        return path.getFileName().toString().endsWith(".json");
    }
}
