package com.eb.javafx.scene;

import com.eb.javafx.save.SaveSnapshotDocument;
import com.eb.javafx.save.SaveSnapshotSection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SceneFlowSnapshotDocumentsTest {
    @Test
    void composesAndRestoresSceneFlowSectionWithAppSections() {
        SceneFlowState state = new SceneFlowState(
                "intro",
                2,
                List.of(new SceneReturnPoint("caller", 1)),
                List.of("choice.a"),
                "menu");

        SaveSnapshotDocument document = SceneFlowSnapshotDocuments.compose(state, List.of(
                new SaveSnapshotSection("appState", 1, "{\"value\":true}")));

        assertTrue(document.section(SceneFlowStateJson.SNAPSHOT_SECTION_ID).isPresent());
        assertTrue(document.section("appState").isPresent());
        SceneFlowState restored = SceneFlowSnapshotDocuments.restore(document);
        assertEquals("intro", restored.activeSceneId());
        assertEquals(List.of("choice.a"), restored.selectedChoiceIds());
    }
}
