package com.eb.javafx.support;

import com.eb.javafx.accessibility.AccessibilityProfile;
import com.eb.javafx.assets.AssetCatalog;
import com.eb.javafx.assets.AssetDefinition;
import com.eb.javafx.assets.AssetType;
import com.eb.javafx.characters.CharacterProfile;
import com.eb.javafx.characters.CharacterRegistry;
import com.eb.javafx.characters.RelationshipState;
import com.eb.javafx.debug.DebugRegistry;
import com.eb.javafx.debug.DebugSnapshot;
import com.eb.javafx.diagnostics.DiagnosticProblem;
import com.eb.javafx.diagnostics.DiagnosticRegistry;
import com.eb.javafx.diagnostics.DiagnosticSeverity;
import com.eb.javafx.events.GameEvent;
import com.eb.javafx.events.GameEventBus;
import com.eb.javafx.input.InputAction;
import com.eb.javafx.input.InputBinding;
import com.eb.javafx.input.InputDevice;
import com.eb.javafx.input.InputMap;
import com.eb.javafx.input.InputTrigger;
import com.eb.javafx.inventory.InventoryCatalog;
import com.eb.javafx.inventory.InventoryItemDefinition;
import com.eb.javafx.inventory.InventoryState;
import com.eb.javafx.journal.JournalEntryDefinition;
import com.eb.javafx.journal.JournalState;
import com.eb.javafx.localization.LocalizedTextBundle;
import com.eb.javafx.localization.LocalizationService;
import com.eb.javafx.progress.ProgressSnapshot;
import com.eb.javafx.progress.ProgressSnapshotCodec;
import com.eb.javafx.progress.ProgressSupport;
import com.eb.javafx.progress.ProgressTracker;
import com.eb.javafx.save.SaveSnapshotSection;
import com.eb.javafx.settings.SettingDefinition;
import com.eb.javafx.settings.SettingType;
import com.eb.javafx.settings.SettingsStore;
import com.eb.javafx.timeline.TimelinePlayer;
import com.eb.javafx.timeline.TimelineSequence;
import com.eb.javafx.timeline.TimelineStatus;
import com.eb.javafx.timeline.TimelineStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GenericSupportModulesTest {

    @Test
    void localizationLooksUpSelectedBundleAndReportsMissingIds() {
        LocalizationService service = new LocalizationService();
        service.registerBundle(new LocalizedTextBundle("en", Map.of("menu.start", "Start")));
        service.registerBundle(new LocalizedTextBundle("fr", Map.of("menu.start", "Démarrer")));

        service.selectLanguage("fr");

        assertEquals("Démarrer", service.textOrId("menu.start"));
        assertEquals("menu.quit", service.textOrId("menu.quit"));
        assertEquals(Set.of("menu.quit"), service.missingTextIds(List.of("menu.start", "menu.quit")));
        assertThrows(IllegalArgumentException.class, () -> service.selectLanguage("missing"));
    }

    @Test
    void assetCatalogValidatesAppOwnedAssetPaths(@TempDir Path root) throws Exception {
        Files.createDirectories(root.resolve("images"));
        Files.writeString(root.resolve("images/background.png"), "demo");
        AssetCatalog catalog = new AssetCatalog();
        catalog.register(new AssetDefinition("background", AssetType.IMAGE, "images/background.png", true, List.of("scene")));
        catalog.register(new AssetDefinition("missing", AssetType.AUDIO, "audio/theme.ogg", false, List.of()));
        catalog.register(new AssetDefinition("escape", AssetType.CONFIG, "../secret.json", false, List.of()));

        assertEquals(List.of("background"), catalog.preloadAssets().stream().map(AssetDefinition::id).toList());
        assertEquals(2, catalog.validateExisting(root).problems().size());
    }

    @Test
    void inputMapResolvesTriggersWithinContext() {
        InputMap inputMap = new InputMap();
        InputTrigger enter = new InputTrigger(InputDevice.KEYBOARD, "ENTER", Set.of());
        inputMap.registerAction(new InputAction("confirm", "Confirm", "menu", true, List.of()));
        inputMap.registerAction(new InputAction("advance", "Advance", "dialogue", true, List.of()));

        inputMap.bind(new InputBinding("confirm", enter));
        inputMap.bind(new InputBinding("advance", enter));

        assertEquals("confirm", inputMap.actionForTrigger("menu", enter).orElseThrow().id());
        assertEquals("advance", inputMap.actionForTrigger("dialogue", enter).orElseThrow().id());
        assertThrows(IllegalArgumentException.class, () -> inputMap.bind(new InputBinding("missing", enter)));
    }

    @Test
    void eventBusPublishesToSubscribersAndKeepsHistory() {
        GameEventBus bus = new GameEventBus();
        List<GameEvent> received = new ArrayList<>();
        Runnable unsubscribe = bus.subscribe("route.changed", received::add);
        GameEvent event = new GameEvent("route.changed", "router", Map.of("route", "main-menu"), Instant.EPOCH);

        bus.publish(event);
        unsubscribe.run();
        bus.publish(new GameEvent("route.changed", "router", Map.of("route", "settings"), Instant.EPOCH));

        assertEquals(List.of(event), received);
        assertEquals(2, bus.history("route.changed").size());
    }

    @Test
    void progressTrackerSupportsRequirementsEffectsAndSaveSnapshots() {
        ProgressTracker tracker = new ProgressTracker();
        assertFalse(ProgressSupport.requireFlag(tracker, "ending-a").evaluate(null).isAllowed());

        ProgressSupport.setFlag(tracker, "ending-a").apply(null);
        tracker.incrementCounter("visits", 2);
        tracker.completeMilestone("intro-complete");
        tracker.unlock("gallery-1");

        assertTrue(ProgressSupport.requireFlag(tracker, "ending-a").evaluate(null).isAllowed());
        ProgressSnapshotCodec codec = new ProgressSnapshotCodec();
        SaveSnapshotSection section = codec.toSection(tracker.snapshot());
        ProgressSnapshot restored = codec.fromSection(section);
        assertEquals(2, restored.counters().get("visits"));
        assertTrue(restored.milestones().contains("intro-complete"));
        assertTrue(restored.unlocks().contains("gallery-1"));
        assertThrows(IllegalArgumentException.class, () ->
                codec.fromSection(new SaveSnapshotSection("other", ProgressSnapshotCodec.SCHEMA_VERSION, section.payloadJson())));
        assertThrows(IllegalArgumentException.class, () ->
                codec.fromSection(new SaveSnapshotSection(ProgressSnapshotCodec.SECTION_ID, 99, section.payloadJson())));
    }

    @Test
    void inventoryCharactersAndJournalStoreGenericGameState() {
        InventoryCatalog items = new InventoryCatalog();
        items.register(new InventoryItemDefinition("key", "Key", "quest", 3, List.of("important")));
        InventoryState inventory = new InventoryState();
        inventory.add("key", 5, items);
        inventory.remove("key", 1);

        CharacterRegistry characters = new CharacterRegistry();
        characters.register(new CharacterProfile("hero", "Hero", List.of("player"), Map.of("voice", "neutral")));
        RelationshipState relationships = new RelationshipState();
        relationships.increment("hero", "trust", 2);

        JournalEntryDefinition entry = new JournalEntryDefinition("quest.find-key", "Find the key", "quest", 10, List.of());
        JournalState journal = new JournalState();
        journal.markRead(entry.id());

        assertEquals(2, inventory.quantity("key"));
        assertEquals("Hero", characters.profile("hero").orElseThrow().displayName());
        assertEquals(2, relationships.value("hero", "trust"));
        assertTrue(journal.status("quest.find-key").read());
    }

    @Test
    void diagnosticsSettingsAccessibilityTimelineAndDebugExposeReusableModels() {
        DiagnosticRegistry diagnostics = new DiagnosticRegistry();
        diagnostics.register(() -> List.of(new DiagnosticProblem("assets", DiagnosticSeverity.ERROR, "Missing file", "hero")));
        assertTrue(diagnostics.runChecks().hasErrors());

        SettingsStore settings = new SettingsStore();
        settings.register(new SettingDefinition("textSpeed", "Text speed", SettingType.INTEGER, "5"));
        settings.set("textSpeed", "8");
        assertEquals("8", settings.value("textSpeed"));

        AccessibilityProfile accessibility = new AccessibilityProfile(1.5, true, true, true, true);
        assertTrue(accessibility.highContrast());

        TimelineSequence sequence = new TimelineSequence("intro", List.of(
                new TimelineStep("fade-in", 100, Map.of()),
                new TimelineStep("hold", 200, Map.of())));
        TimelinePlayer player = new TimelinePlayer(sequence);
        player.play();
        player.advance(100);
        assertEquals("hold", player.currentStep().orElseThrow().id());
        player.advance(200);
        assertEquals(TimelineStatus.COMPLETED, player.status());

        DebugRegistry debug = new DebugRegistry();
        debug.register(() -> new DebugSnapshot("State", Map.of("route", "main-menu")));
        assertEquals("main-menu", debug.snapshots().get(0).rows().get("route"));
    }
}
