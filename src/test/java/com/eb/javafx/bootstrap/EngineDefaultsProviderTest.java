package com.eb.javafx.bootstrap;

import com.eb.javafx.content.EnginePlaceholderContentModule;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.routing.DefaultRouteModule;
import com.eb.javafx.routing.RouteModule;
import com.eb.javafx.scene.EnginePlaceholderSceneModule;
import com.eb.javafx.scene.SceneModule;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class EngineDefaultsProviderTest {

    /** Collecting context that records the add*Module calls and rejects the rest. */
    private static final class Collector implements ModuleContext {
        final List<StaticContentModule> statics = new ArrayList<>();
        final List<SceneModule> scenes = new ArrayList<>();
        final List<RouteModule> routes = new ArrayList<>();
        @Override public void addStaticContentModule(StaticContentModule m) { statics.add(m); }
        @Override public void addSceneModule(SceneModule m) { scenes.add(m); }
        @Override public void addRouteModule(RouteModule m) { routes.add(m); }
        @Override public Path applicationRoot() { throw new UnsupportedOperationException(); }
        @Override public ApplicationResourceConfig resourceConfig() { throw new UnsupportedOperationException(); }
        @Override public com.eb.javafx.resources.ResourceRegistry resourceRegistry() { throw new UnsupportedOperationException(); }
        @Override public Path providerAssetBase() { throw new UnsupportedOperationException(); }
        @Override public ResourceRoots resourceRoots() { throw new UnsupportedOperationException(); }
        @Override public FontRegistrar fonts() { throw new UnsupportedOperationException(); }
        @Override public void addStylesheet(String url) { throw new UnsupportedOperationException(); }
    }

    @Test
    void identityIsLowestPriority() {
        EngineDefaultsProvider provider = new EngineDefaultsProvider();
        assertEquals("engine-defaults", provider.id());
        assertEquals(Integer.MIN_VALUE, provider.priority());
    }

    @Test
    void contributesPlaceholderContentSceneAndDefaultRoute() {
        Collector collector = new Collector();
        new EngineDefaultsProvider().contribute(collector);

        assertEquals(1, collector.statics.size());
        assertInstanceOf(EnginePlaceholderContentModule.class, collector.statics.get(0));
        assertEquals(1, collector.scenes.size());
        assertInstanceOf(EnginePlaceholderSceneModule.class, collector.scenes.get(0));
        assertEquals(1, collector.routes.size());
        assertInstanceOf(DefaultRouteModule.class, collector.routes.get(0));
    }
}
