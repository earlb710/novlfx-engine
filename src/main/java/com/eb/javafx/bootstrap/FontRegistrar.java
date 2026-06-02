package com.eb.javafx.bootstrap;

import javafx.scene.text.Font;

/**
 * Lets an {@link EngineModuleProvider} register fonts bundled inside its own module. Loading through
 * the provider's class loader is what avoids JPMS encapsulation when the engine (a different module)
 * would otherwise be unable to read the provider's in-jar font.
 */
public interface FontRegistrar {

    /**
     * Loads + registers a font bundled in the provider's module, resolving {@code resourcePath}
     * against {@code owner}'s class loader. Registering the family (so it resolves in CSS) is a side
     * effect of loading. Returns the loaded {@link Font}.
     *
     * @param owner        a class from the provider's module (e.g. the provider itself)
     * @param resourcePath classpath resource path to the font; a leading {@code /} is optional
     * @param size         font size in points (irrelevant to family registration)
     */
    Font registerFromModule(Class<?> owner, String resourcePath, double size);

    /**
     * Registers an already-loaded {@link Font} (e.g. one the provider loaded itself). Loading via
     * {@link Font#loadFont} already registers the family globally; this hook lets the host track the
     * contribution.
     */
    void register(Font font);
}
