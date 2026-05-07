package com.eb.javafx.save;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.progress.ProgressSnapshot;
import com.eb.javafx.progress.ProgressSnapshotCodec;
import com.eb.javafx.scene.SceneCheckpoint;
import com.eb.javafx.scene.SceneCheckpointLog;
import com.eb.javafx.scene.SceneCheckpointLogJson;
import com.eb.javafx.scene.SceneCheckpointPayload;
import com.eb.javafx.scene.SceneFlowState;
import com.eb.javafx.scene.SceneFlowStateJson;
import com.eb.javafx.scene.SceneReturnPoint;
import com.eb.javafx.scene.SceneStepType;
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
        SceneFlowState sceneFlowState = new SceneFlowState(
                "intro",
                2,
                List.of(new SceneReturnPoint("caller", 1)),
                List.of("choice.accept"),
                null);
        SceneCheckpointLog checkpointLog = new SceneCheckpointLog(List.of(
                new SceneCheckpoint(
                        0,
                        sceneFlowState,
                        "intro",
                        2,
                        "choice",
                        SceneStepType.CHOICE,
                        SceneCheckpointPayload.choiceSelection("choice.accept", "choice.accept"),
                        false,
                        false,
                        Map.of("source", "test"))), 0, -1, false);
        ReusableGameplaySnapshot snapshot = new ReusableGameplaySnapshot(
                sceneFlowState,
                checkpointLog,
                new GameDateTime(3, "evening"),
                new ProgressSnapshot(Set.of("met.character"), Map.of("trust", 2), Set.of("intro.done"), Set.of("route.office")),
                new InventorySnapshot(Map.of("keycard", 1)),
                new WardrobeSnapshot(Set.of("jacket"), Map.of("travel", Map.of("body", "jacket"))),
                new CharacterStatesSnapshot(List.of(new CharacterSnapshot(
                        "alex",
                        "template.alex",
                        Map.of("energy", 7),
                        Map.of("player", 3),
                        Set.of("available"),
                        Map.of("display", "alex.default")))),
                new JournalSnapshot(Set.of("quest.intro"), Set.of("quest.intro")),
                new LocationOccupancySnapshot(Map.of("alex", "office")));

        SaveSnapshotDocument document = ReusableGameplaySnapshotDocuments.compose(snapshot, List.of(
                new SaveSnapshotSection("appState", 1, "{\"domain\":\"owned-by-app\"}")));

        assertTrue(document.section(SceneFlowStateJson.SNAPSHOT_SECTION_ID).isPresent());
        assertTrue(document.section(SceneCheckpointLogJson.SNAPSHOT_SECTION_ID).isPresent());
        assertTrue(document.section(ProgressSnapshotCodec.SECTION_ID).isPresent());
        assertTrue(document.section(InventorySnapshotCodec.SECTION_ID).isPresent());
        assertTrue(document.section(WardrobeSnapshotCodec.SECTION_ID).isPresent());
        assertTrue(document.section(CharacterStatesSnapshotCodec.SECTION_ID).isPresent());
        assertTrue(document.section(JournalSnapshotCodec.SECTION_ID).isPresent());
        assertTrue(document.section(LocationOccupancySnapshotCodec.SECTION_ID).isPresent());
        assertTrue(document.section("appState").isPresent());

        ReusableGameplaySnapshot restored = ReusableGameplaySnapshotDocuments.restore(document);
        assertEquals("intro", restored.sceneFlowState().activeSceneId());
        assertEquals(2, restored.sceneFlowState().stepIndex());
        assertEquals(1, restored.sceneCheckpointLog().checkpoints().size());
        assertEquals("choice.accept", restored.sceneCheckpointLog().currentCheckpoint().payload().choiceId());
        assertEquals(3, restored.gameTime().day());
        assertEquals("evening", restored.gameTime().timeSlotId());
        assertTrue(restored.progress().flags().contains("met.character"));
        assertEquals(2, restored.progress().counters().get("trust"));
        assertTrue(restored.progress().milestones().contains("intro.done"));
        assertTrue(restored.progress().unlocks().contains("route.office"));
        assertEquals(1, restored.inventory().quantities().get("keycard"));
        assertTrue(restored.wardrobe().unlockedWearableIds().contains("jacket"));
        assertEquals("jacket", restored.wardrobe().outfits().get("travel").get("body"));
        assertEquals("alex", restored.characters().characters().get(0).characterId());
        assertEquals(7, restored.characters().characters().get(0).stats().get("energy"));
        assertTrue(restored.journal().readEntryIds().contains("quest.intro"));
        assertEquals("office", restored.locationOccupancy().characterLocations().get("alex"));
        assertEquals(1, restored.inventory().toState().quantity("keycard"));
        assertTrue(restored.wardrobe().toState().isUnlocked("jacket"));
        assertEquals(3, restored.characters().toStates().get(0).relationship("player"));
        assertTrue(restored.journal().toState().status("quest.intro").read());
        assertEquals("office", restored.locationOccupancy().toState().locationOf("alex").orElseThrow());
    }

    @Test
    void threeArgumentSnapshotConstructorDefaultsExtendedSectionsToEmptySnapshots() {
        ReusableGameplaySnapshot snapshot = new ReusableGameplaySnapshot(
                new SceneFlowState("intro", 0, List.of(), List.of(), null),
                new GameDateTime(1, "morning"),
                new ProgressSnapshot(Set.of(), Map.of(), Set.of(), Set.of()));

        SaveSnapshotDocument document = ReusableGameplaySnapshotDocuments.compose(snapshot, List.of());
        ReusableGameplaySnapshot restored = ReusableGameplaySnapshotDocuments.restore(document);

        assertTrue(document.section(SceneCheckpointLogJson.SNAPSHOT_SECTION_ID).isPresent());
        assertTrue(restored.sceneCheckpointLog().checkpoints().isEmpty());
        assertTrue(restored.inventory().quantities().isEmpty());
        assertTrue(restored.wardrobe().unlockedWearableIds().isEmpty());
        assertTrue(restored.characters().characters().isEmpty());
        assertTrue(restored.journal().unlockedEntryIds().isEmpty());
        assertTrue(restored.locationOccupancy().characterLocations().isEmpty());
    }

    @Test
    void restoreDefaultsMissingCheckpointSectionToEmptyLogForOlderSaves() {
        ReusableGameplaySnapshot snapshot = new ReusableGameplaySnapshot(
                new SceneFlowState("intro", 0, List.of(), List.of(), null),
                new GameDateTime(1, "morning"),
                new ProgressSnapshot(Set.of(), Map.of(), Set.of(), Set.of()));
        SaveSnapshotDocument currentDocument = ReusableGameplaySnapshotDocuments.compose(snapshot, List.of());
        SaveSnapshotDocument olderDocument = new SaveSnapshotDocument(currentDocument.sections().stream()
                .filter(section -> !SceneCheckpointLogJson.SNAPSHOT_SECTION_ID.equals(section.sectionId()))
                .toList());

        ReusableGameplaySnapshot restored = ReusableGameplaySnapshotDocuments.restore(olderDocument);

        assertTrue(restored.sceneCheckpointLog().checkpoints().isEmpty());
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
