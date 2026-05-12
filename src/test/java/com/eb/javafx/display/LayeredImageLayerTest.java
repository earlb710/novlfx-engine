package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class LayeredImageLayerTest {

    @Test
    void constructsWithNameAndVariants() {
        LayeredImageLayer layer = new LayeredImageLayer("expression", List.of(
            new LayeredImageVariant("expr/happy.png", "flag:hero_happy"),
            new LayeredImageVariant("expr/neutral.png", null)
        ));
        assertEquals("expression", layer.name());
        assertEquals(2, layer.variants().size());
    }

    @Test
    void variantsListIsImmutable() {
        LayeredImageLayer layer = new LayeredImageLayer("body", List.of(
            new LayeredImageVariant("body/default.png", null)
        ));
        assertThrows(UnsupportedOperationException.class, () -> layer.variants().add(
            new LayeredImageVariant("body/alt.png", null)));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
            () -> new LayeredImageLayer("", List.of(new LayeredImageVariant("img.png", null))));
    }

    @Test
    void rejectsEmptyVariants() {
        assertThrows(IllegalArgumentException.class,
            () -> new LayeredImageLayer("body", List.of()));
    }
}
