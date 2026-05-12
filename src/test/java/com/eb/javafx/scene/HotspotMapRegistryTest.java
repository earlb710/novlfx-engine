package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

final class HotspotMapRegistryTest {

    private static HotspotDefinition hotspot(String id) {
        return new HotspotDefinition(id, "loc." + id, 0.1, 0.1, 0.2, 0.2, null, "scene_" + id);
    }

    private static HotspotMapDefinition map(String id) {
        return new HotspotMapDefinition(id, "bg_" + id + ".png", List.of(hotspot("h1")));
    }

    @Test
    void findsRegisteredMap() {
        HotspotMapRegistry registry = new HotspotMapRegistry();
        registry.register(map("town"));
        Optional<HotspotMapDefinition> found = registry.find("town");
        assertTrue(found.isPresent());
        assertEquals("town", found.get().id());
    }

    @Test
    void returnsEmptyForUnknownId() {
        HotspotMapRegistry registry = new HotspotMapRegistry();
        assertTrue(registry.find("missing").isEmpty());
    }

    @Test
    void requireThrowsForUnknownId() {
        HotspotMapRegistry registry = new HotspotMapRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.require("missing"));
    }

    @Test
    void requireReturnsKnownMap() {
        HotspotMapRegistry registry = new HotspotMapRegistry();
        registry.register(map("forest"));
        HotspotMapDefinition result = registry.require("forest");
        assertEquals("forest", result.id());
        assertEquals("bg_forest.png", result.backgroundImageRef());
    }

    @Test
    void mapDefinitionExposesHotspots() {
        HotspotDefinition h = hotspot("cave");
        HotspotMapDefinition def = new HotspotMapDefinition("map1", "bg.png", List.of(h));
        assertEquals(1, def.hotspots().size());
        assertEquals("cave", def.hotspots().get(0).id());
    }

    @Test
    void rejectsBlankMapId() {
        assertThrows(IllegalArgumentException.class,
            () -> new HotspotMapDefinition("", "bg.png", List.of()));
    }

    @Test
    void rejectsBlankBackgroundImageRef() {
        assertThrows(IllegalArgumentException.class,
            () -> new HotspotMapDefinition("map1", "", List.of()));
    }
}
