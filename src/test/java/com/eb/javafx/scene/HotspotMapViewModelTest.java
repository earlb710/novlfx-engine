package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class HotspotMapViewModelTest {

    @Test
    void constructsOptionViewModel() {
        HotspotOptionViewModel opt = new HotspotOptionViewModel("cave", "loc.cave", 0.1, 0.2, 0.3, 0.4, true);
        assertEquals("cave", opt.id());
        assertEquals("loc.cave", opt.labelTextKey());
        assertEquals(0.1, opt.x());
        assertEquals(0.2, opt.y());
        assertEquals(0.3, opt.width());
        assertEquals(0.4, opt.height());
        assertTrue(opt.enabled());
    }

    @Test
    void disabledOptionViewModel() {
        HotspotOptionViewModel opt = new HotspotOptionViewModel("locked", "loc.locked", 0.5, 0.5, 0.1, 0.1, false);
        assertFalse(opt.enabled());
    }

    @Test
    void constructsMapViewModel() {
        HotspotOptionViewModel opt = new HotspotOptionViewModel("town", "loc.town", 0.1, 0.1, 0.2, 0.2, true);
        HotspotMapViewModel vm = new HotspotMapViewModel("bg_world.png", List.of(opt));
        assertEquals("bg_world.png", vm.backgroundImageRef());
        assertEquals(1, vm.options().size());
        assertEquals("town", vm.options().get(0).id());
    }

    @Test
    void mapViewModelOptionsAreImmutable() {
        HotspotMapViewModel vm = new HotspotMapViewModel("bg.png", List.of());
        assertThrows(UnsupportedOperationException.class, () -> vm.options().add(
            new HotspotOptionViewModel("x", "loc.x", 0.1, 0.1, 0.1, 0.1, true)));
    }
}
