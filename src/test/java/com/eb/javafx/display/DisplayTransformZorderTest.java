package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DisplayTransformZorderTest {

    @Test
    void defaultZorderIsZero() {
        DisplayTransform t = new DisplayTransform("t", 100, 100, 1.0, 0.5, 0.5);
        assertEquals(0, t.zorder());
    }

    @Test
    void zorderConstructorSetsField() {
        DisplayTransform t = new DisplayTransform("t", 100, 100, 1.0, 0.5, 0.5, 3);
        assertEquals(3, t.zorder());
    }

    @Test
    void withZorderReturnsCopyWithNewZorder() {
        DisplayTransform t = new DisplayTransform("t", 100, 100, 1.0, 0.5, 0.5);
        DisplayTransform updated = t.withZorder(5);
        assertEquals(5, updated.zorder());
        assertEquals(0, t.zorder());
        assertEquals("t", updated.id());
    }

    @Test
    void negativeZorderIsAllowed() {
        DisplayTransform t = new DisplayTransform("t", 100, 100, 1.0, 0.5, 0.5, -1);
        assertEquals(-1, t.zorder());
    }
}
