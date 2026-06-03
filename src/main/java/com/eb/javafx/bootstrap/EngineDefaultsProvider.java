package com.eb.javafx.bootstrap;

import com.eb.javafx.content.EnginePlaceholderContentModule;
import com.eb.javafx.routing.DefaultRouteModule;
import com.eb.javafx.scene.EnginePlaceholderSceneModule;

/**
 * The engine's built-in baseline, expressed as a lowest-priority {@link EngineModuleProvider}: the
 * placeholder content + scene modules and the default route module that every game gets for free.
 *
 * <p>This is an <strong>internal</strong> provider — it is <em>not</em> declared as a
 * {@code ServiceLoader} service, so a consumer's discovery never picks it up. Instead
 * {@link BootstrapService} composes it ahead of the application-supplied modules on every boot path
 * (discovery-assembled or not), so the baseline is always present and registered first. Its
 * {@link #priority()} of {@link Integer#MIN_VALUE} documents that it contributes before any other
 * provider.</p>
 */
public final class EngineDefaultsProvider implements EngineModuleProvider {

    @Override
    public String id() {
        return "engine-defaults";
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;   // always first
    }

    @Override
    public void contribute(ModuleContext context) {
        context.addStaticContentModule(new EnginePlaceholderContentModule());
        context.addSceneModule(new EnginePlaceholderSceneModule());
        context.addRouteModule(new DefaultRouteModule());
    }
}
