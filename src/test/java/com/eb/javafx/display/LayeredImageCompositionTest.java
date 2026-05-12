package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class LayeredImageCompositionTest {

    @Test
    void constructsWithDefinitionIdAndEntries() {
        List<LayeredCompositionEntry> entries = List.of(
            new LayeredCompositionEntry("body", "body/default.png"),
            new LayeredCompositionEntry("expression", "expr/happy.png")
        );
        LayeredImageComposition comp = new LayeredImageComposition("hero-composite", entries);
        assertEquals("hero-composite", comp.definitionId());
        assertEquals(2, comp.entries().size());
    }

    @Test
    void entryExposesLayerNameAndImageRef() {
        LayeredCompositionEntry entry = new LayeredCompositionEntry("hair", "hair/long.png");
        assertEquals("hair", entry.layerName());
        assertEquals("hair/long.png", entry.imageRef());
    }

    @Test
    void entriesListIsImmutable() {
        LayeredImageComposition comp = new LayeredImageComposition("c", List.of());
        assertThrows(UnsupportedOperationException.class,
            () -> comp.entries().add(new LayeredCompositionEntry("body", "img.png")));
    }
}
