package com.eb.javafx.resources;

import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Small IO helpers that bridge {@link URL} resource references and the UTF-8 string content that engine JSON
 * loaders consume. Centralizes the {@code openStream}/{@code readAllBytes} dance and provides a best-effort
 * {@link Path} view for filesystem URLs (used by callers that still want filesystem semantics like writing back).
 */
public final class ResourceIo {
    private ResourceIo() {
    }

    /** Reads the UTF-8 contents of a resource URL. */
    public static String readString(URL url) {
        Validation.requireNonNull(url, "Resource URL is required.");
        try (InputStream stream = url.openStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read resource: " + url, exception);
        }
    }

    /** Returns the URL as a filesystem {@link Path} when its scheme is {@code file:}, otherwise empty. */
    public static Optional<Path> toFilesystemPath(URL url) {
        Validation.requireNonNull(url, "Resource URL is required.");
        if (!"file".equalsIgnoreCase(url.getProtocol())) {
            return Optional.empty();
        }
        try {
            return Optional.of(Paths.get(url.toURI()));
        } catch (URISyntaxException exception) {
            return Optional.empty();
        }
    }

    /** Converts a {@link Path} to a {@link URL}; throws on malformed paths. */
    public static URL toUrl(Path path) {
        Validation.requireNonNull(path, "Path is required.");
        try {
            return path.toUri().toURL();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to derive URL from path: " + path, exception);
        }
    }
}
