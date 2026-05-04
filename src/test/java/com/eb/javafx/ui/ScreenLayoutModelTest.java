package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ScreenLayoutModelTest {
    @Test
    void copiesReusableLayoutCollections() {
        List<ScreenLayoutSection> sections = new ArrayList<>(List.of(
                new ScreenLayoutSection("body", "Body", List.of("Ready"))));
        List<ScreenActionViewModel> actions = new ArrayList<>(List.of(
                new ScreenActionViewModel("Start", "start", true)));

        ScreenLayoutModel model = new ScreenLayoutModel(
                ScreenLayoutType.TITLED_PANEL,
                "Title",
                "Subtitle",
                sections,
                actions,
                List.of(),
                List.of(),
                "Footer");
        sections.clear();
        actions.clear();

        assertEquals(1, model.contentSections().size());
        assertEquals(1, model.primaryActions().size());
    }

    @Test
    void validatesReusableLayoutRequiredData() {
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenLayoutSection("", "Body", List.of("Ready")));
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenLayoutSection("body", "Body", List.of(" ")));
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenLayoutModel(null, "Title", null, List.of(), List.of(), List.of(), List.of(), null));
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenLayoutModel(ScreenLayoutType.TITLED_PANEL, "", null, List.of(), List.of(), List.of(), List.of(), null));
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenLayoutModel(ScreenLayoutType.TITLED_PANEL, "Title", " ", List.of(), List.of(), List.of(), List.of(), null));
    }
}
