package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class HotspotDefinitionTest {

    @Test
    void constructsWithRequiredFields() {
        HotspotDefinition h = new HotspotDefinition("village", "loc.village", 0.1, 0.2, 0.3, 0.4, null, "scene_village");
        assertEquals("village", h.id());
        assertEquals("loc.village", h.labelTextKey());
        assertEquals(0.1, h.x());
        assertEquals(0.2, h.y());
        assertEquals(0.3, h.width());
        assertEquals(0.4, h.height());
        assertTrue(h.conditionExpression().isEmpty());
        assertEquals("scene_village", h.targetSceneId());
    }

    @Test
    void constructsWithConditionExpression() {
        HotspotDefinition h = new HotspotDefinition("cave", "loc.cave", 0.5, 0.5, 0.2, 0.2, "flag:cave_unlocked", "scene_cave");
        assertEquals("flag:cave_unlocked", h.conditionExpression().get());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
            () -> new HotspotDefinition("", "loc.x", 0.1, 0.1, 0.1, 0.1, null, "scene_x"));
    }

    @Test
    void rejectsNonPositiveWidth() {
        assertThrows(IllegalArgumentException.class,
            () -> new HotspotDefinition("h", "loc.h", 0.0, 0.0, 0.0, 0.2, null, "scene_h"));
    }

    @Test
    void rejectsBoundsExceedingOne() {
        assertThrows(IllegalArgumentException.class,
            () -> new HotspotDefinition("h", "loc.h", 0.0, 0.0, 1.5, 0.2, null, "scene_h"));
    }

    @Test
    void rejectsBlankTargetSceneId() {
        assertThrows(IllegalArgumentException.class,
            () -> new HotspotDefinition("h", "loc.h", 0.1, 0.1, 0.2, 0.2, null, ""));
    }
}
