package com.lr2alt.javafx.display;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Resolves display assets from the checked-out game tree.
 */
public final class GameAssetLocator {
    private final Path repoRoot;
    private List<Path> indexedFiles;

    public GameAssetLocator(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public Optional<Path> resolve(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalize(sourcePath);
        Path gameRoot = repoRoot.resolve("game");
        Path direct = gameRoot.resolve(normalized);
        if (Files.exists(direct)) {
            return Optional.of(direct.normalize());
        }

        String fileName = Path.of(normalized).getFileName().toString().toLowerCase(Locale.ROOT);
        return indexedFiles().stream()
                .sorted(Comparator.comparingInt(path -> relativeGamePath(path).length()))
                .filter(path -> {
                    String relative = relativeGamePath(path).toLowerCase(Locale.ROOT);
                    return relative.endsWith(normalized.toLowerCase(Locale.ROOT))
                            || path.getFileName().toString().toLowerCase(Locale.ROOT).equals(fileName);
                })
                .findFirst();
    }

    private List<Path> indexedFiles() {
        if (indexedFiles == null) {
            indexedFiles = new ArrayList<>();
            Path gameRoot = repoRoot.resolve("game");
            if (Files.isDirectory(gameRoot)) {
                try (Stream<Path> stream = Files.walk(gameRoot)) {
                    stream.filter(Files::isRegularFile)
                            .filter(this::isSupportedImage)
                            .forEach(indexedFiles::add);
                } catch (IOException exception) {
                    throw new IllegalStateException("Unable to index game assets from " + gameRoot, exception);
                }
            }
        }
        return indexedFiles;
    }

    private boolean isSupportedImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp");
    }

    private String relativeGamePath(Path path) {
        return normalize(repoRoot.resolve("game").relativize(path).toString());
    }

    private String normalize(String path) {
        return path.replace('\\', '/');
    }
}
