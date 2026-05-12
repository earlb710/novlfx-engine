package com.eb.javafx.routing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class OverlayDescriptorTest {

    @Test
    void overlayDescriptorStoresFields() {
        RouteFactory factory = ctx -> null;
        OverlayDescriptor descriptor = new OverlayDescriptor("hud", factory, true);
        assertEquals("hud", descriptor.id());
        assertEquals(factory, descriptor.factory());
        assertTrue(descriptor.initiallyVisible());
    }

    @Test
    void overlayDescriptorRejectsNullId() {
        assertThrows(Exception.class, () -> new OverlayDescriptor(null, ctx -> null, true));
    }

    @Test
    void overlayDescriptorHiddenByDefault() {
        OverlayDescriptor descriptor = new OverlayDescriptor("notification", ctx -> null, false);
        assertFalse(descriptor.initiallyVisible());
    }
}
