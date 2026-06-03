package com.example.patterns;

import com.eb.javafx.bootstrap.EngineModuleProvider;
import com.eb.javafx.bootstrap.ModuleContext;
import com.eb.javafx.resources.ResourceCategory;

/**
 * Pattern 2 — A provider that ships its own in-jar resources (fonts, CSS, images).
 *
 * <p>The golden rule: a provider <strong>owns its in-jar resources</strong> — it loads / references
 * them through <em>its own</em> module so JPMS encapsulation never blocks the engine (a different
 * module). The hooks below all take the provider's class / class loader for exactly that reason.</p>
 *
 * <p>For these classpath resources to be readable cross-module, keep them in a <em>resource-only</em>
 * package (a folder with no {@code .class} files) <strong>or</strong> {@code opens} the package in
 * this module's {@code module-info.java} — mirroring the engine's own {@code opens com.eb.javafx.ui;}.
 * On-disk resources (backgrounds, external CSS) are filesystem I/O and are not affected by JPMS.</p>
 */
public final class ResourceContributingProvider implements EngineModuleProvider {

    public ResourceContributingProvider() {
    }

    @Override
    public void contribute(ModuleContext context) {
        // A font bundled in THIS jar — loaded via this module's class loader, so its family resolves
        // in CSS. (getClass() identifies this provider's module.)
        context.fonts().registerFromModule(getClass(), "/com/example/patterns/fonts/Game.ttf", 12.0);

        // A stylesheet bundled in this jar — build the URL yourself (it carries this module's loader)
        // and hand it over; it is applied to every themed scene, after the engine theme + footer CSS.
        context.addStylesheet(getClass().getResource("/com/example/patterns/theme.css").toExternalForm());

        // An additional image search root in this jar, registered with this module's class loader so
        // the engine's resource registry can resolve files under it.
        context.resourceRoots().addClasspathRoot(
                ResourceCategory.IMAGES,
                "classpath:/com/example/patterns/images",
                getClass().getClassLoader());

        // …then the game's actual content/scene/route modules:
        // context.addStaticContentModule(new MyContent());
    }
}
