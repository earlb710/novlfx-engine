package com.eb.javafx.save;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.progress.ProgressSnapshot;
import com.eb.javafx.progress.ProgressSnapshotCodec;
import com.eb.javafx.scene.SceneFlowState;
import com.eb.javafx.scene.SceneFlowStateJson;
import com.eb.javafx.scene.SceneReturnPoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReusableGameplaySnapshotDocumentsTest {
    @Test
    void composesAndRestoresReusableGameplaySlicesWithAppOwnedSections() {
        ReusableGameplaySnapshot snapshot = new ReusableGameplaySnapshot(
                new SceneFlowState("intro", 2, List.of(new SceneReturnPoint("caller", 1)), List.of("choice.accept"), null),
                new GameDateTime(3, "evening"),
                new ProgressSnapshot(Set.of("met.character"), Map.of("trust", 2), Set.of("intro.done"), Set.of("route.office")));

        SaveSnapshotDocument document = ReusableGameplaySnapshotDocuments.compose(snapshot, List.of(
                new SaveSnapshotSection("appState", 1, "{\"domain\":\"owned-by-app\"}")));

        assertTrue(document.section(SceneFlowStateJson.SNAPSHOT_SECTION_ID).isPresent());
        assertTrue(document.section(ProgressSnapshotCodec.SECTION_ID).isPresent());
        assertTrue(document.section("appState").isPresent());

        ReusableGameplaySnapshot restored = ReusableGameplaySnapshotDocuments.restore(document);
        assertEquals("intro", restored.sceneFlowState().activeSceneId());
        assertEquals(2, restored.sceneFlowState().stepIndex());
        assertEquals(3, restored.gameTime().day());
        assertEquals("evening", restored.gameTime().timeSlotId());
        assertTrue(restored.progress().flags().contains("met.character"));
        assertEquals(2, restored.progress().counters().get("trust"));
        assertTrue(restored.progress().milestones().contains("intro.done"));
        assertTrue(restored.progress().unlocks().contains("route.office"));
    }

    @Test
    void reportsMissingReusableGameplaySections() {
        SaveSnapshotDocument document = new SaveSnapshotDocument(List.of(
                new SaveSnapshotSection("appState", 1, "{}")));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ReusableGameplaySnapshotDocuments.restore(document));

        assertTrue(error.getMessage().contains("Missing required save snapshot section"));
    }
}
