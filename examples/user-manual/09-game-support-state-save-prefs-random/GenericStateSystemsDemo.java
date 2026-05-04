import com.eb.javafx.characters.CharacterProfile;
import com.eb.javafx.characters.CharacterRegistry;
import com.eb.javafx.characters.RelationshipState;
import com.eb.javafx.debug.DebugRegistry;
import com.eb.javafx.debug.DebugSnapshot;
import com.eb.javafx.inventory.InventoryCatalog;
import com.eb.javafx.inventory.InventoryItemDefinition;
import com.eb.javafx.inventory.InventoryState;
import com.eb.javafx.journal.JournalEntryDefinition;
import com.eb.javafx.journal.JournalState;
import com.eb.javafx.timeline.TimelinePlayer;
import com.eb.javafx.timeline.TimelineSequence;
import com.eb.javafx.timeline.TimelineStatus;
import com.eb.javafx.timeline.TimelineStep;

import java.util.List;
import java.util.Map;

/**
 * Demonstrates content-neutral state models for inventory, characters, journal entries, timelines, and debug views.
 *
 * <p>Expected output prints item quantities, profile/relationship values, journal read state, timeline status,
 * and reusable debug rows that an application-owned debug menu could render.</p>
 */
public final class GenericStateSystemsDemo {
    private GenericStateSystemsDemo() {
    }

    public static void main(String[] args) {
        InventoryCatalog inventoryCatalog = new InventoryCatalog();
        inventoryCatalog.register(new InventoryItemDefinition("key", "Key", "quest", 3, List.of("important")));
        InventoryState inventory = new InventoryState();
        inventory.add("key", 5, inventoryCatalog);
        inventory.remove("key", 1);
        System.out.println("Key quantity after stack cap/remove: " + inventory.quantity("key"));

        CharacterRegistry characterRegistry = new CharacterRegistry();
        characterRegistry.register(new CharacterProfile(
                "hero",
                "Hero",
                List.of("player"),
                Map.of("displayReference", "hero-neutral")));
        RelationshipState relationships = new RelationshipState();
        relationships.increment("hero", "trust", 2);
        System.out.println("Character profile name: " + characterRegistry.profile("hero").orElseThrow().displayName());
        System.out.println("Hero trust: " + relationships.value("hero", "trust"));

        JournalEntryDefinition quest = new JournalEntryDefinition(
                "quest.find-key",
                "Find the key",
                "quest",
                10,
                List.of("tutorial"));
        JournalState journal = new JournalState();
        journal.markRead(quest.id());
        System.out.println("Journal entry read: " + journal.status(quest.id()).read());

        TimelineSequence sequence = new TimelineSequence("intro-title", List.of(
                new TimelineStep("fade-in", 400, Map.of("target", "title")),
                new TimelineStep("hold", 800, Map.of("target", "title")),
                new TimelineStep("fade-out", 400, Map.of("target", "title"))));
        TimelinePlayer player = new TimelinePlayer(sequence);
        player.play();
        player.advance(450);
        System.out.println("Current timeline step: " + player.currentStep().orElseThrow().id());
        player.pause();
        System.out.println("Paused timeline status: " + player.status());
        player.resume();
        player.advance(1200);
        System.out.println("Completed timeline status: " + player.status().equals(TimelineStatus.COMPLETED));

        DebugRegistry debugRegistry = new DebugRegistry();
        debugRegistry.register(() -> new DebugSnapshot("State", Map.of(
                "inventory.key", Integer.toString(inventory.quantity("key")),
                "relationship.hero.trust", Integer.toString(relationships.value("hero", "trust")),
                "journal.quest.find-key.read", Boolean.toString(journal.status(quest.id()).read()),
                "timeline.status", player.status().name())));
        System.out.println("Debug snapshot rows: " + debugRegistry.snapshots().get(0).rows());
    }
}
