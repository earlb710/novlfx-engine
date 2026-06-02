package com.eb.javafx.bootstrap;

import com.eb.javafx.resources.ResourceCategory;

import java.nio.file.Path;

/**
 * Narrow facade an {@link EngineModuleProvider} uses to contribute resource search roots, layered
 * into the engine's {@link com.eb.javafx.resources.ResourceRegistry}. Roots are queried in insertion
 * order (first match wins), with provider/application roots intended to precede the library's own.
 *
 * <p>Classpath roots are resolved with the provider's <em>own</em> class loader so its in-jar
 * resources are reachable cross-module — which requires the resources to live in a resource-only
 * package (no {@code .class} files) or an {@code opens}'d package of the provider's module.</p>
 */
public interface ResourceRoots {

    /** Adds a classpath search root for {@code category}, resolved against {@code classLoader}
     *  (typically {@code SomeProvider.class.getClassLoader()}). */
    ResourceRoots addClasspathRoot(ResourceCategory category, String classpathPath, ClassLoader classLoader);

    /** Adds a filesystem search root directory for {@code category}. Missing directories are
     *  skipped, so optional roots don't fail when a provider ships no content for a category. */
    ResourceRoots addFilesystemRoot(ResourceCategory category, Path directory);
}
