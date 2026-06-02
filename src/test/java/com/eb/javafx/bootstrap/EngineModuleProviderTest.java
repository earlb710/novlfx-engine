package com.eb.javafx.bootstrap;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EngineModuleProviderTest {

    /** A minimal provider relying entirely on the interface defaults. */
    private static final class DefaultProvider implements EngineModuleProvider {
        @Override
        public void contribute(ModuleContext context) {
            // no-op
        }
    }

    /** A provider overriding identity / ordering. */
    private static final class CustomProvider implements EngineModuleProvider {
        @Override public String id() { return "custom"; }
        @Override public int priority() { return -5; }
        @Override public Set<String> dependsOn() { return Set.of("base"); }
        @Override public void contribute(ModuleContext context) { }
    }

    @Test
    void defaultsAreClassNameZeroPriorityNoDeps() {
        EngineModuleProvider provider = new DefaultProvider();
        assertEquals(DefaultProvider.class.getName(), provider.id());
        assertEquals(0, provider.priority());
        assertTrue(provider.dependsOn().isEmpty());
    }

    @Test
    void overridesAreHonoured() {
        EngineModuleProvider provider = new CustomProvider();
        assertEquals("custom", provider.id());
        assertEquals(-5, provider.priority());
        assertEquals(Set.of("base"), provider.dependsOn());
    }

    @Test
    void contributeReceivesTheSuppliedContext() {
        AtomicReference<ModuleContext> seen = new AtomicReference<>();
        EngineModuleProvider provider = seen::set;   // contribute(ctx) -> seen.set(ctx)
        ModuleContext ctx = new ModuleContext() {
            @Override public java.nio.file.Path applicationRoot() { return java.nio.file.Path.of("."); }
            @Override public ApplicationResourceConfig resourceConfig() { return ApplicationResourceConfig.defaults(); }
            @Override public java.nio.file.Path providerAssetBase() { return java.nio.file.Path.of("."); }
            @Override public void addStaticContentModule(com.eb.javafx.content.StaticContentModule m) { }
            @Override public void addSceneModule(com.eb.javafx.scene.SceneModule m) { }
            @Override public void addRouteModule(com.eb.javafx.routing.RouteModule m) { }
            @Override public ResourceRoots resourceRoots() { return null; }
            @Override public FontRegistrar fonts() { return null; }
            @Override public void addStylesheet(String url) { }
        };
        provider.contribute(ctx);
        assertSame(ctx, seen.get());
    }
}
