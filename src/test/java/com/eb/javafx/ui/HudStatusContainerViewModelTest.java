package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class HudStatusContainerViewModelTest {
    @Test
    void containerCarriesGroupedReadOnlyStatusRows() {
        HudStatusContainerViewModel model = new HudStatusContainerViewModel(
                "HUD",
                true,
                0.75,
                10,
                List.of(new HudStatusGroupViewModel("session", "Session", List.of(
                        new HudStatusRowViewModel("Day", "1", true)))));

        assertEquals("Session", model.groups().get(0).title());
        assertEquals("Day", model.groups().get(0).rows().get(0).label());
    }

    @Test
    void containerValidatesOpacity() {
        assertThrows(IllegalArgumentException.class, () ->
                new HudStatusContainerViewModel("HUD", true, 2.0, 0, List.of()));
    }
}
