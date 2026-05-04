package com.eb.javafx.support;

import com.eb.javafx.characters.CharacterStatBlock;
import com.eb.javafx.characters.CharacterState;
import com.eb.javafx.characters.CharacterTemplate;
import com.eb.javafx.characters.CharacterTemplateRegistry;
import com.eb.javafx.content.ContentPackDescriptor;
import com.eb.javafx.debug.DebugPanelDescriptor;
import com.eb.javafx.diagnostics.DiagnosticDescriptor;
import com.eb.javafx.diagnostics.DiagnosticSeverity;
import com.eb.javafx.events.GameCommand;
import com.eb.javafx.events.GameCommandDispatcher;
import com.eb.javafx.events.GameEvent;
import com.eb.javafx.events.GameEventBus;
import com.eb.javafx.events.GameEventQueue;
import com.eb.javafx.gamesupport.CodeDefinition;
import com.eb.javafx.gamesupport.CodeTableDefinition;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.gamesupport.GenericDescriptor;
import com.eb.javafx.gamesupport.GenericDescriptorRegistry;
import com.eb.javafx.gamesupport.LocationDescriptor;
import com.eb.javafx.gamesupport.LocationMovementService;
import com.eb.javafx.gamesupport.LocationOccupancy;
import com.eb.javafx.gamesupport.LocationRegistry;
import com.eb.javafx.gamesupport.MovementValidationResult;
import com.eb.javafx.gamesupport.TimeAdvanceService;
import com.eb.javafx.gamesupport.TimeSaveSnapshots;
import com.eb.javafx.gamesupport.TimeScheduledCommand;
import com.eb.javafx.gamesupport.TimeScheduler;
import com.eb.javafx.inventory.InventoryCatalog;
import com.eb.javafx.inventory.InventoryItemDefinition;
import com.eb.javafx.inventory.OutfitState;
import com.eb.javafx.inventory.WardrobeCatalog;
import com.eb.javafx.inventory.WardrobeState;
import com.eb.javafx.inventory.WearableDefinition;
import com.eb.javafx.inventory.WearableSlotDefinition;
import com.eb.javafx.messages.MessageEntry;
import com.eb.javafx.messages.MessageThreadState;
import com.eb.javafx.messages.Notification;
import com.eb.javafx.messages.NotificationState;
import com.eb.javafx.organizations.OrganizationDescriptor;
import com.eb.javafx.organizations.ProductionOrder;
import com.eb.javafx.organizations.ProductionQueue;
import com.eb.javafx.organizations.ResourceLedger;
import com.eb.javafx.save.SaveSnapshotSection;
import com.eb.javafx.util.GenericValidation;
import com.eb.javafx.util.ImportIssueSeverity;
import com.eb.javafx.util.ImportValidationIssue;
import com.eb.javafx.util.ImportValidationReport;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Section2BacklogSupportTest {

    @Test
    void registersGenericDescriptorsAndDescriptorModels() {
        GenericDescriptorRegistry registry = new GenericDescriptorRegistry();
        registry.register(new GenericDescriptor("descriptor.alpha", "synthetic", "Alpha", List.of("demo"), Map.of("scope", "test")));

        assertEquals("Alpha", registry.descriptor("descriptor.alpha").orElseThrow().title());
        assertEquals(List.of("descriptor.alpha"), registry.descriptorsByKind("synthetic").stream().map(GenericDescriptor::id).toList());
        assertEquals(List.of("descriptor.alpha"), registry.descriptorsWithTag("demo").stream().map(GenericDescriptor::id).toList());
        registry.requireKnownIds(List.of("descriptor.alpha"));
        assertThrows(IllegalArgumentException.class, () -> registry.requireKnownIds(List.of("missing")));

        ContentPackDescriptor pack = new ContentPackDescriptor("pack.demo", "Demo Pack", "1.0", List.of("core"), Map.of());
        DiagnosticDescriptor diagnostic = new DiagnosticDescriptor("diag.demo", "Demo Diagnostic", DiagnosticSeverity.WARNING, List.of("startup"), Map.of());
        DebugPanelDescriptor panel = new DebugPanelDescriptor("debug.demo", "Debug Demo", "debug-route", Map.of());
        assertEquals("pack.demo", pack.id());
        assertEquals(DiagnosticSeverity.WARNING, diagnostic.defaultSeverity());
        assertEquals("debug-route", panel.routeId());
    }

    @Test
    void storesCharacterTemplatesStateStatsAndRelationships() {
        CharacterTemplateRegistry registry = new CharacterTemplateRegistry();
        CharacterTemplate template = new CharacterTemplate(
                "template.synthetic",
                "Synthetic Character",
                "archetype.generic",
                CharacterStatBlock.empty().withValue("focus", 2),
                List.of("fixture"),
                Map.of());
        registry.register(template);

        CharacterState state = new CharacterState("character.synthetic", "template.synthetic", template.baseStats());
        state.incrementStat("focus", 3);
        state.incrementRelationship("ally", 4);
        state.addFlag("introduced");
        state.putMetadata("mood", "neutral");

        assertEquals(5, state.stats().value("focus"));
        assertEquals(4, state.relationship("ally"));
        assertTrue(state.hasFlag("introduced"));
        assertEquals("neutral", state.metadata().get("mood"));
        assertEquals("Synthetic Character", registry.template("template.synthetic").orElseThrow().displayName());
    }

    @Test
    void advancesTimeRunsHooksCommandsAndSnapshots() {
        CodeTableDefinition slots = new CodeTableDefinition("slots", "Slots", List.of(
                new CodeDefinition("a", "A", 0, List.of()),
                new CodeDefinition("b", "B", 1, List.of())));
        GameClock clock = new GameClock(slots, "a");
        TimeScheduler scheduler = new TimeScheduler(slots);
        scheduler.schedule(new TimeScheduledCommand("cmd", new GameDateTime(1, "b"), "notify", Map.of("id", "n1")));
        List<String> hookLog = new ArrayList<>();
        TimeAdvanceService service = new TimeAdvanceService(clock, scheduler, List.of(new com.eb.javafx.gamesupport.TimeAdvanceHook() {
            @Override
            public void beforeAdvance(GameDateTime currentTime) {
                hookLog.add("before:" + currentTime.timeSlotId());
            }

            @Override
            public void afterAdvance(GameDateTime previousTime, GameDateTime currentTime) {
                hookLog.add("after:" + currentTime.timeSlotId());
            }
        }));
        List<TimeScheduledCommand> delivered = new ArrayList<>();

        GameDateTime current = service.advanceSlot(delivered::add);
        SaveSnapshotSection section = TimeSaveSnapshots.toSnapshotSection(current);

        assertEquals("b", current.timeSlotId());
        assertEquals(List.of("before:a", "after:b"), hookLog);
        assertEquals("cmd", delivered.get(0).id());
        assertEquals("b", TimeSaveSnapshots.fromSnapshotSection(section).timeSlotId());
    }

    @Test
    void queuesEventsAndDispatchesCommands() {
        GameEventQueue queue = new GameEventQueue();
        queue.enqueue(new GameEvent("synthetic.event", "source", Map.of(), Instant.EPOCH));
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.drain().size());

        GameCommandDispatcher dispatcher = new GameCommandDispatcher();
        dispatcher.register("synthetic.command", command -> List.of(new GameEvent("synthetic.completed", command.id(), command.payload(), Instant.EPOCH)));
        GameEventBus bus = new GameEventBus();
        List<GameEvent> observed = new ArrayList<>();
        bus.subscribe("synthetic.completed", observed::add);

        List<GameEvent> emitted = dispatcher.dispatch(
                new GameCommand("cmd-1", "synthetic.command", "target", Map.of("value", "1"), Instant.EPOCH),
                bus);

        assertEquals("synthetic.completed", emitted.get(0).type());
        assertEquals(1, observed.size());
    }

    @Test
    void validatesMovementAndWardrobeState() {
        LocationRegistry locations = new LocationRegistry();
        locations.register(new LocationDescriptor("lobby", "Lobby", "route.lobby", null, List.of(), List.of()));
        locations.register(new LocationDescriptor("lab", "Lab", "route.lab", "lobby", List.of(), List.of()));
        LocationOccupancy occupancy = new LocationOccupancy();
        occupancy.place("character.synthetic", "lobby", locations);
        LocationMovementService movement = new LocationMovementService(
                locations,
                occupancy,
                List.of((characterId, from, to) -> "lab".equals(to) ? MovementValidationResult.permit() : MovementValidationResult.blocked("blocked")));
        assertTrue(movement.move("character.synthetic", "lab").allowed());
        assertEquals("lab", occupancy.locationOf("character.synthetic").orElseThrow());

        InventoryCatalog inventory = new InventoryCatalog();
        inventory.register(new InventoryItemDefinition("jacket", "Synthetic Jacket", "wearable", 1, List.of()));
        WardrobeCatalog wardrobe = new WardrobeCatalog();
        wardrobe.registerSlot(new WearableSlotDefinition("torso", "Torso", true));
        wardrobe.registerWearable(new WearableDefinition("jacket", List.of("torso"), List.of("casual"), Map.of()), inventory);
        OutfitState outfit = new OutfitState();
        outfit.equip("torso", "jacket", wardrobe);
        WardrobeState wardrobeState = new WardrobeState();
        wardrobeState.unlock("jacket", wardrobe);
        wardrobeState.saveOutfit("default", outfit);

        assertEquals("jacket", wardrobeState.outfit("default").orElseThrow().equippedItem("torso").orElseThrow());
        assertTrue(wardrobeState.isUnlocked("jacket"));
    }

    @Test
    void tracksMessagesNotificationsOrganizationsResourcesAndImports() {
        NotificationState notifications = new NotificationState();
        notifications.add(new Notification("n1", "system", "Title", "Body", Instant.EPOCH, false, Map.of()));
        assertEquals(1, notifications.unread().size());
        notifications.markRead("n1");
        assertTrue(notifications.unread().isEmpty());

        MessageThreadState thread = new MessageThreadState("thread.synthetic", List.of("a", "b"), Map.of());
        thread.append(new MessageEntry("m1", "a", "Hello", Instant.EPOCH, Map.of()), true);
        assertEquals(1, thread.unreadCount());
        thread.markRead();
        assertEquals(0, thread.unreadCount());

        OrganizationDescriptor organization = new OrganizationDescriptor("org.synthetic", "Synthetic Org", List.of("a"), List.of("fixture"), Map.of());
        ResourceLedger ledger = new ResourceLedger();
        ledger.add("credits", 10);
        ledger.spend("credits", 4);
        ProductionQueue queue = new ProductionQueue();
        queue.enqueue(ProductionOrder.create("order1", "widget", 2, Map.of("owner", organization.id())));

        assertEquals(6, ledger.balance("credits"));
        assertTrue(queue.advance(2).get(0).complete());

        ImportValidationReport report = new ImportValidationReport(
                "synthetic-import",
                1,
                List.of(GenericValidation.missingReference("root.items[0]", "item", "missing")));
        assertFalse(report.successful());
        assertEquals(ImportIssueSeverity.ERROR, report.issues().get(0).severity());
        GenericValidation.requireKnownIds(List.of("known"), Set.of("known"), "descriptor");
        assertThrows(IllegalArgumentException.class, () -> GenericValidation.requireKnownIds(List.of("missing"), Set.of("known"), "descriptor"));
        assertEquals("note", new ImportValidationIssue(ImportIssueSeverity.INFO, null, "note").message());
    }
}
