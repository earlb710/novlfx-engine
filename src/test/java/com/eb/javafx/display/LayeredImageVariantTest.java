package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LayeredImageVariantTest {

    @Test
    void constructsWithImageRefOnly() {
        LayeredImageVariant v = new LayeredImageVariant("body/default.png", null);
        assertEquals("body/default.png", v.imageRef());
        assertTrue(v.conditionExpression().isEmpty());
    }

    @Test
    void constructsWithCondition() {
        LayeredImageVariant v = new LayeredImageVariant("body/happy.png", "flag:hero_happy");
        assertEquals("flag:hero_happy", v.conditionExpression().get());
    }

    @Test
    void rejectsBlankImageRef() {
        assertThrows(IllegalArgumentException.class,
            () -> new LayeredImageVariant("", "flag:x"));
    }
}
