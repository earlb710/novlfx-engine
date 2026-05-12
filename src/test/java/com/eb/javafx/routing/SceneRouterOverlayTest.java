package com.eb.javafx.routing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SceneRouterOverlayTest {

    @Test
    void registerAndShowOverlay() {
        SceneRouter router = new SceneRouter();
        router.registerOverlay(new OverlayDescriptor("hud", ctx -> null, false));
        assertFalse(router.isOverlayVisible("hud"));
        router.showOverlay("hud");
        assertTrue(router.isOverlayVisible("hud"));
    }

    @Test
    void hideOverlaySetsInvisible() {
        SceneRouter router = new SceneRouter();
        router.registerOverlay(new OverlayDescriptor("hud", ctx -> null, true));
        assertTrue(router.isOverlayVisible("hud"));
        router.hideOverlay("hud");
        assertFalse(router.isOverlayVisible("hud"));
    }

    @Test
    void activeOverlaysReturnsVisibleIds() {
        SceneRouter router = new SceneRouter();
        router.registerOverlay(new OverlayDescriptor("hud", ctx -> null, true));
        router.registerOverlay(new OverlayDescriptor("notification", ctx -> null, false));
        assertEquals(1, router.activeOverlays().size());
        assertTrue(router.activeOverlays().contains("hud"));
    }

    @Test
    void showUnknownOverlayThrows() {
        SceneRouter router = new SceneRouter();
        assertThrows(IllegalArgumentException.class, () -> router.showOverlay("nonexistent"));
    }

    @Test
    void hideUnknownOverlayThrows() {
        SceneRouter router = new SceneRouter();
        assertThrows(IllegalArgumentException.class, () -> router.hideOverlay("nonexistent"));
    }
}
