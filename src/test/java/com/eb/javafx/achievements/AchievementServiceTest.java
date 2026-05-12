// src/test/java/com/eb/javafx/achievements/AchievementServiceTest.java
package com.eb.javafx.achievements;

import com.eb.javafx.events.GameEvent;
import com.eb.javafx.events.GameEventBus;
import com.eb.javafx.progress.ProgressTracker;
import com.eb.javafx.scene.SceneConditionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class AchievementServiceTest {

    private ProgressTracker progress;
    private SceneConditionEvaluator evaluator;
    private GameEventBus eventBus;
    private AchievementRegistry registry;
    private AchievementState state;
    private AchievementService service;

    @BeforeEach
    void setUp() {
        progress = new ProgressTracker();
        evaluator = new SceneConditionEvaluator(progress);
        eventBus = new GameEventBus();
        registry = new AchievementRegistry();
        state = new AchievementState();
        service = new AchievementService(registry, state, evaluator, eventBus);
    }

    @Test
    void achievementDefinitionStoresFields() {
        AchievementDefinition def = new AchievementDefinition(
                "first-win", "ach.first.name", "ach.first.desc",
                Optional.of("icons/first.png"), "flag:won");
        assertEquals("first-win", def.id());
        assertEquals("ach.first.name", def.nameTextKey());
        assertEquals("ach.first.desc", def.descriptionTextKey());
        assertEquals("icons/first.png", def.iconRef().orElseThrow());
        assertEquals("flag:won", def.unlockConditionExpression());
    }

    @Test
    void achievementDefinitionAcceptsEmptyIcon() {
        AchievementDefinition def = new AchievementDefinition(
                "first-win", "ach.first.name", "ach.first.desc", Optional.empty(), "flag:won");
        assertTrue(def.iconRef().isEmpty());
    }

    @Test
    void checkAllDoesNotUnlockWhenConditionFalse() {
        registry.register(new AchievementDefinition(
                "win", "ach.win.name", "ach.win.desc", Optional.empty(), "flag:won"));
        service.checkAll();
        assertFalse(state.isUnlocked("win"));
    }

    @Test
    void checkAllUnlocksWhenConditionMet() {
        progress.setFlag("won", true);
        registry.register(new AchievementDefinition(
                "win", "ach.win.name", "ach.win.desc", Optional.empty(), "flag:won"));
        service.checkAll();
        assertTrue(state.isUnlocked("win"));
    }

    @Test
    void checkAllEmitsEventOnUnlock() {
        progress.setFlag("won", true);
        AtomicReference<GameEvent> captured = new AtomicReference<>();
        eventBus.subscribe(AchievementUnlockedEvent.EVENT_TYPE, captured::set);
        registry.register(new AchievementDefinition(
                "win", "ach.win.name", "ach.win.desc", Optional.empty(), "flag:won"));
        service.checkAll();
        assertNotNull(captured.get());
        assertEquals("win", captured.get().payload().get("achievementId"));
    }

    @Test
    void alreadyUnlockedAchievementDoesNotEmitAgain() {
        progress.setFlag("won", true);
        state.unlock("win");
        AtomicReference<GameEvent> captured = new AtomicReference<>();
        eventBus.subscribe(AchievementUnlockedEvent.EVENT_TYPE, captured::set);
        registry.register(new AchievementDefinition(
                "win", "ach.win.name", "ach.win.desc", Optional.empty(), "flag:won"));
        service.checkAll();
        assertNull(captured.get());
    }

    @Test
    void snapshotRoundTripsViaCodec() {
        state.unlock("win");
        state.unlock("first-dialogue");
        AchievementSnapshotCodec codec = new AchievementSnapshotCodec();
        String json = codec.toJson(state.snapshot());
        AchievementSnapshot restored = codec.fromJson(json, "test");
        assertTrue(restored.unlockedIds().contains("win"));
        assertTrue(restored.unlockedIds().contains("first-dialogue"));
        assertEquals(2, restored.unlockedIds().size());
    }

    @Test
    void achievementStateRestoreFromSnapshot() {
        AchievementSnapshot snapshot = new AchievementSnapshot(Set.of("win", "first-dialogue"));
        AchievementState restored = new AchievementState();
        restored.restore(snapshot);
        assertTrue(restored.isUnlocked("win"));
        assertTrue(restored.isUnlocked("first-dialogue"));
        assertFalse(restored.isUnlocked("other"));
    }

    @Test
    void achievementRegistryRejectsDuplicateId() {
        registry.register(new AchievementDefinition(
                "win", "n", "d", Optional.empty(), "flag:won"));
        assertThrows(IllegalArgumentException.class, () ->
                registry.register(new AchievementDefinition(
                        "win", "n2", "d2", Optional.empty(), "flag:won")));
    }
}
