package com.eb.javafx.display;

import com.eb.javafx.util.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Resolves display assets from the checked-out game tree.
 *
 * <p>The locator expects a repository root containing a {@code game} directory.
 * It first tries a direct normalized path, then lazily indexes supported image
 * files and matches by suffix or filename; unresolved or blank paths return
 * {@link Optional#empty()} rather than throwing.</p>
 */
public final class GameAssetLocator {
    private final Path repoRoot;
    private List<Path> indexedFiles;

    /**
     * Creates a locator rooted at the checked-out repository directory.
     *
     * @param repoRoot path whose {@code game} child contains migrated assets
     */
    public GameAssetLocator(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    /**
     * Resolves an authored display source path to an existing image file.
     *
     * @param sourcePath relative game asset path using either slash style
     * @return existing normalized path when found, otherwise empty
     */
    public Optional<Path> resolve(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return Optional.empty();
        }

        String normalized = PathUtils.normalizeSeparators(sourcePath);
        Path gameRoot = repoRoot.resolve("game");
        Path direct = PathUtils.resolveChild(gameRoot, normalized);
        if (Files.exists(direct)) {
            return Optional.of(direct.normalize());
        }

        String fileName = PathUtils.fileNameLowercase(normalized);
        return indexedFiles().stream()
                .sorted(Comparator.comparingInt(path -> relativeGamePath(path).length()))
                .filter(path -> {
                    String relative = relativeGamePath(path).toLowerCase();
                    return relative.endsWith(normalized.toLowerCase())
                            || PathUtils.fileNameLowercase(path.toString()).equals(fileName);
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
        String extension = PathUtils.extensionLowercase(path);
        return extension.equals(".png") || extension.equals(".jpg") || extension.equals(".jpeg") || extension.equals(".webp");
    }

    private String relativeGamePath(Path path) {
        return PathUtils.normalizeSeparators(repoRoot.resolve("game").relativize(path).toString());
    }
}
