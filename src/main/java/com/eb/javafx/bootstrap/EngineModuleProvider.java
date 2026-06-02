package com.eb.javafx.bootstrap;

import java.util.Set;

/**
 * Extension-discovery entry point: a project contributes engine content/scene/route modules (and
 * their resources) by implementing this interface and declaring it as a {@link java.util.ServiceLoader}
 * provider — {@code provides com.eb.javafx.bootstrap.EngineModuleProvider with ...} in a modular
 * consumer, or a {@code META-INF/services/com.eb.javafx.bootstrap.EngineModuleProvider} file on the
 * classpath. The engine discovers providers, orders them deterministically, and invokes
 * {@link #contribute(ModuleContext)} once each during bootstrap assembly.
 *
 * <p>Implementations must have a public no-arg constructor (the {@link java.util.ServiceLoader}
 * requirement). Heavy construction is deferred to {@link #contribute(ModuleContext)}, which receives
 * the resolved application root + configuration so a provider can build modules that need them.</p>
 *
 * <p><strong>Status:</strong> the SPI types exist (Phase 1) but are not yet wired into the boot
 * sequence; see {@code docs/SPI_PLAN.md}.</p>
 */
public interface EngineModuleProvider {

    /** Stable identity used for de-duplication and {@link #dependsOn()} ordering. Defaults to the
     *  implementing class's fully-qualified name. */
    default String id() {
        return getClass().getName();
    }

    /** Contribution order — providers with a lower value contribute first. Ties keep
     *  discovery/insertion order. Defaults to {@code 0}. */
    default int priority() {
        return 0;
    }

    /** Ids of providers that must contribute before this one (a finer-grained ordering than
     *  {@link #priority()}). Defaults to none. */
    default Set<String> dependsOn() {
        return Set.of();
    }

    /** Registers this provider's modules and resources through {@code context}. Invoked once during
     *  bootstrap assembly, after the application root and configuration are resolved. */
    void contribute(ModuleContext context);
}
