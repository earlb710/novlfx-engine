package com.eb.javafx.resources;

import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Indexed catalog of files discovered under one or more roots per {@link ResourceCategory}.
 *
 * <p>Supports two root kinds:</p>
 * <ul>
 *     <li><b>Classpath roots</b>: written in config as {@code classpath:/com/eb/javafx/...}; resolved through the
 *     supplied {@link ClassLoader} and walked even when the classes ship inside a JAR.</li>
 *     <li><b>Filesystem roots</b>: written as relative or absolute filesystem paths; resolved against an
 *     application-supplied base directory (typically the working directory).</li>
 * </ul>
 *
 * <p>Lookups are <b>strictly category-isolated</b>: a {@link ResourceCategory#IMAGES} lookup never falls through to
 * any other category. Within a category, roots are queried in <b>insertion order</b>; the first occurrence of a
 * relative file name wins. Application-supplied roots are intended to be added before the library's own roots so
 * application files can override library files of the same relative name.</p>
 */
public final class ResourceRegistry {
    public static final String CLASSPATH_PREFIX = "classpath:";

    private final Map<ResourceCategory, LinkedHashMap<String, URL>> indexByCategory;

    private ResourceRegistry(EnumMap<ResourceCategory, LinkedHashMap<String, URL>> indexByCategory) {
        EnumMap<ResourceCategory, LinkedHashMap<String, URL>> copy = new EnumMap<>(ResourceCategory.class);
        for (ResourceCategory category : ResourceCategory.values()) {
            LinkedHashMap<String, URL> entries = indexByCategory.get(category);
            copy.put(category, entries == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(entries));
        }
        this.indexByCategory = Collections.unmodifiableMap(copy);
    }

    /** Returns the URL of the first matching file in the category, or empty. */
    public Optional<URL> find(ResourceCategory category, String relativeName) {
        Validation.requireNonNull(category, "Resource category is required.");
        String key = normalizeKey(Validation.requireNonBlank(relativeName, "Relative name is required."));
        return Optional.ofNullable(indexByCategory.get(category).get(key));
    }

    /** Returns the URL of the first matching file or throws if none is registered. */
    public URL require(ResourceCategory category, String relativeName) {
        return find(category, relativeName).orElseThrow(() -> new IllegalArgumentException(
                "No resource registered for category " + category.configKey() + " and name " + relativeName));
    }

    /** Returns all relative file names indexed under the given category, in insertion order. */
    public List<String> names(ResourceCategory category) {
        Validation.requireNonNull(category, "Resource category is required.");
        return List.copyOf(indexByCategory.get(category).keySet());
    }

    /** Returns the URLs of all files indexed under the given category, in insertion order. */
    public List<URL> list(ResourceCategory category) {
        Validation.requireNonNull(category, "Resource category is required.");
        return List.copyOf(indexByCategory.get(category).values());
    }

    /** Returns true when the category has at least one indexed file. */
    public boolean hasAny(ResourceCategory category) {
        Validation.requireNonNull(category, "Resource category is required.");
        return !indexByCategory.get(category).isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder that walks each registered root and indexes every regular file it finds.
     *
     * <p>Roots are walked in the order they are added. Within a category, the first occurrence of a relative file
     * name wins. Applications should add their own roots before adding the library's roots so application files
     * override library files when both exist.</p>
     */
    public static final class Builder {
        private final EnumMap<ResourceCategory, LinkedHashMap<String, URL>> indexByCategory =
                new EnumMap<>(ResourceCategory.class);

        private Builder() {
            for (ResourceCategory category : ResourceCategory.values()) {
                indexByCategory.put(category, new LinkedHashMap<>());
            }
        }

        /**
         * Adds a root described by a config string. {@code classpath:} prefixed values resolve through the supplied
         * class loader; everything else is treated as a filesystem path resolved against {@code filesystemBase}.
         *
         * <p>Missing filesystem roots are silently skipped so optional application roots do not fail when an
         * application has not authored content for a category yet. Missing classpath roots are also silently skipped
         * so a library JAR can omit categories without breaking startup.</p>
         */
        public Builder addRoot(ResourceCategory category, String rootSpec, Path filesystemBase, ClassLoader classLoader) {
            Validation.requireNonNull(category, "Resource category is required.");
            String spec = Validation.requireNonBlank(rootSpec, "Resource root spec is required.").trim();
            if (spec.startsWith(CLASSPATH_PREFIX)) {
                addClasspathRoot(category, spec.substring(CLASSPATH_PREFIX.length()),
                        classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader);
            } else {
                Path base = filesystemBase == null ? Paths.get("").toAbsolutePath().normalize() : filesystemBase;
                Path resolved = base.resolve(spec).toAbsolutePath().normalize();
                addFilesystemRoot(category, resolved);
            }
            return this;
        }

        /** Adds a classpath root such as {@code /com/eb/javafx/fonts}. */
        public Builder addClasspathRoot(ResourceCategory category, String classpathPath, ClassLoader classLoader) {
            Validation.requireNonNull(category, "Resource category is required.");
            String normalized = stripLeadingSlash(Validation.requireNonBlank(
                    classpathPath, "Classpath path is required."));
            ClassLoader loader = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
            try {
                Enumeration<URL> resources = loader.getResources(normalized);
                if (!resources.hasMoreElements()) {
                    return this;
                }
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    walkClasspathUrl(category, url);
                }
            } catch (IOException exception) {
                throw new UncheckedIOException(
                        "Failed to enumerate classpath root for " + category.configKey() + ": " + classpathPath,
                        exception);
            }
            return this;
        }

        /** Adds a filesystem root. Missing directories are silently skipped. */
        public Builder addFilesystemRoot(ResourceCategory category, Path directory) {
            Validation.requireNonNull(category, "Resource category is required.");
            Validation.requireNonNull(directory, "Filesystem root is required.");
            if (!Files.isDirectory(directory)) {
                return this;
            }
            walkAndIndex(category, directory, directory);
            return this;
        }

        public ResourceRegistry build() {
            return new ResourceRegistry(indexByCategory);
        }

        private void walkClasspathUrl(ResourceCategory category, URL url) {
            URI uri;
            try {
                uri = url.toURI();
            } catch (URISyntaxException exception) {
                throw new IllegalArgumentException("Invalid classpath URL: " + url, exception);
            }
            String scheme = uri.getScheme();
            if ("file".equalsIgnoreCase(scheme)) {
                Path root = Paths.get(uri);
                if (Files.isDirectory(root)) {
                    walkAndIndex(category, root, root);
                }
                return;
            }
            if ("jar".equalsIgnoreCase(scheme)) {
                walkJarUri(category, uri);
                return;
            }
            throw new IllegalArgumentException("Unsupported classpath URL scheme: " + uri);
        }

        private void walkJarUri(ResourceCategory category, URI jarUri) {
            String fragment = jarUri.toString();
            int separator = fragment.indexOf("!/");
            if (separator < 0) {
                throw new IllegalArgumentException("Malformed jar URI: " + jarUri);
            }
            URI jarFileUri;
            try {
                jarFileUri = new URI(fragment.substring(0, separator));
            } catch (URISyntaxException exception) {
                throw new IllegalArgumentException("Invalid jar file URI: " + jarUri, exception);
            }
            String entryPath = fragment.substring(separator + 1);
            FileSystem opened = null;
            FileSystem fileSystem;
            try {
                fileSystem = FileSystems.getFileSystem(jarFileUri);
            } catch (FileSystemNotFoundException notFound) {
                try {
                    fileSystem = FileSystems.newFileSystem(jarFileUri, Map.of());
                    opened = fileSystem;
                } catch (FileSystemAlreadyExistsException race) {
                    fileSystem = FileSystems.getFileSystem(jarFileUri);
                } catch (IOException exception) {
                    throw new UncheckedIOException("Unable to open jar filesystem: " + jarFileUri, exception);
                }
            }
            try {
                Path root = fileSystem.getPath(entryPath);
                if (Files.isDirectory(root)) {
                    walkAndIndex(category, root, root);
                }
            } finally {
                if (opened != null) {
                    try {
                        opened.close();
                    } catch (IOException ignored) {
                        // closing is best effort; URLs cached above survive the close.
                    }
                }
            }
        }

        private void walkAndIndex(ResourceCategory category, Path root, Path keyBase) {
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile).forEach(file -> indexFile(category, keyBase, file));
            } catch (IOException exception) {
                throw new UncheckedIOException("Unable to walk resource root: " + root, exception);
            }
        }

        private void indexFile(ResourceCategory category, Path keyBase, Path file) {
            String key = keyBase.relativize(file).toString().replace('\\', '/');
            URL url;
            try {
                url = file.toUri().toURL();
            } catch (MalformedURLException exception) {
                throw new IllegalStateException("Unable to convert path to URL: " + file, exception);
            }
            indexByCategory.get(category).putIfAbsent(key, url);
        }
    }

    private static String normalizeKey(String relativeName) {
        String trimmed = relativeName.trim();
        String forward = trimmed.replace('\\', '/');
        return forward.startsWith("/") ? forward.substring(1) : forward;
    }

    private static String stripLeadingSlash(String value) {
        return value.startsWith("/") ? value.substring(1) : value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ResourceRegistry registry && indexByCategory.equals(registry.indexByCategory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexByCategory);
    }

    @Override
    public String toString() {
        List<String> parts = new ArrayList<>();
        for (ResourceCategory category : ResourceCategory.values()) {
            parts.add(category.configKey() + "=" + indexByCategory.get(category).size());
        }
        return "ResourceRegistry[" + String.join(", ", parts) + "]";
    }
}
