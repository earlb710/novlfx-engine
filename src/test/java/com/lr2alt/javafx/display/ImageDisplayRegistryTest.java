package com.lr2alt.javafx.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ImageDisplayRegistryTest {

    @Test
    void registerBaseDisplayContentRegistersOnlyReusableDisplayInfrastructure() {
        ImageDisplayRegistry registry = new ImageDisplayRegistry();

        registry.registerBaseDisplayContent();
        registry.validateDisplayContent();

        assertTrue(registry.transforms().isEmpty());
        assertTrue(registry.images().isEmpty());
        assertTrue(registry.layeredCharacters().isEmpty());
        assertEquals(1, registry.animations().size());
    }

    @Test
    void registryViewsCannotBeMutatedByCallers() {
        ImageDisplayRegistry registry = new ImageDisplayRegistry();
        registry.registerBaseDisplayContent();

        assertThrows(UnsupportedOperationException.class, () ->
                registry.images().put("changed", new ImageAssetDefinition("changed", "changed.png", null, DisplayLayer.HUD)));
        assertThrows(UnsupportedOperationException.class, () ->
                registry.transforms().put("changed", new DisplayTransform("changed", 1, 1, 1.0, 0.5, 0.5)));
        assertThrows(UnsupportedOperationException.class, () ->
                registry.animations().clear());
        assertThrows(UnsupportedOperationException.class, () ->
                registry.layeredCharacters().clear());
    }

    @Test
    void imageLookupThrowsForMissingAsset() {
        ImageDisplayRegistry registry = new ImageDisplayRegistry();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                registry.image("missing"));

        assertEquals("Missing required image asset: missing", exception.getMessage());
    }

    @Test
    void transformRejectsInvalidSize() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new DisplayTransform("bad", 0, 18, 1.0, 0.5, 0.5));

        assertEquals("Display transform size must be positive.", exception.getMessage());
    }

    @Test
    void layeredDisplayRejectsEmptyDrawOrder() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new LayeredCharacterDefinition("bad", java.util.List.of(), null, java.util.Map.of()));

        assertEquals("Layered display draw order is required.", exception.getMessage());
    }
}
