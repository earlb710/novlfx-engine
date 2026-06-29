package com.eb.javafx.state;

import com.eb.javafx.characters.CharacterState;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.gamesupport.LocationOccupancy;
import com.eb.javafx.inventory.InventoryState;
import com.eb.javafx.inventory.WardrobeState;
import com.eb.javafx.journal.JournalState;
import com.eb.javafx.progress.ProgressTracker;
import com.eb.javafx.save.CharacterStatesSnapshot;
import com.eb.javafx.save.GameplayStateSnapshot;
import com.eb.javafx.save.InventorySnapshot;
import com.eb.javafx.save.JournalSnapshot;
import com.eb.javafx.save.LocationOccupancySnapshot;
import com.eb.javafx.save.SaveSnapshotSection;
import com.eb.javafx.save.WardrobeSnapshot;
import com.eb.javafx.text.DialogHistory;
import com.eb.javafx.util.Validation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Minimal mutable game state placeholder for the JavaFX bootstrap slice.
 *
 * <p>Visual novel default variables are per-save values. This object represents the
 * JavaFX replacement location for those mutable defaults once gameplay systems are
 * migrated into Java domain classes.</p>
 */
public final class GameState {
    private final String startupRoute;
    private final DialogHistory conversationHistory;
    private ProgressTracker progress = new ProgressTracker();
    private InventoryState inventory = new InventoryState();
    private WardrobeState wardrobe = new WardrobeState();
    private final Map<String, CharacterState> characterStates = new LinkedHashMap<>();
    private JournalState journal = new JournalState();
    private LocationOccupancy locationOccupancy = new LocationOccupancy();
    private final Map<String, CustomRollbackSnapshotSection> customRollbackSections = new LinkedHashMap<>();

    /** Creates placeholder mutable state with the route selected by static content. */
    public GameState(String startupRoute) {
        this(startupRoute, new DialogHistory());
    }

    /** Creates placeholder mutable state with explicit reusable conversation history. */
    public GameState(String startupRoute, DialogHistory conversationHistory) {
        this.startupRoute = startupRoute;
        this.conversationHistory = Validation.requireNonNull(conversationHistory, "Conversation history is required.");
        // Install this GameState's history as the engine-shared instance so every
        // DialogEntriesView constructed via its no-arg constructor (the common AltLife
        // path) routes its writes through this same object — the one the snapshot
        // codec reads when saving the dialog history section.  Without this pairing,
        // the widget would create a private history per construction and the save
        // section would persist an empty engine-instance history, with the widget's
        // actual content lost across save/load.  See DialogHistory.installShared.
        DialogHistory.installShared(this.conversationHistory);
    }

    /**
     * Returns the route a new game should enter after startup.
     *
     * <p>Future mutable gameplay fields should live alongside this value so save
     * data remains explicit rather than hidden in globals.</p>
     */
    public String startupRoute() {
        return startupRoute;
    }

    /** Returns the per-save conversation history used by helper screens and authored UI. */
    public DialogHistory conversationHistory() {
        // Delegate to DialogHistory.shared() rather than returning the constructor-
        // captured field directly.  These can be different instances when something
        // (a widget constructed before this GameState, or another GameState
        // construction that fired first) installed a different instance as shared:
        // DialogEntriesView's no-arg constructor and the save-section supplier /
        // consumer all route through DialogHistory.shared(), so returning the field
        // here would diverge from those callers and the conversation-history screen
        // (which reads conversationHistory()) would see the empty constructor field
        // while widgets and the save codec write into the shared instance.
        // installShared is first-wins, so in normal boot order this returns exactly
        // the instance passed to this GameState's constructor — consistent with the
        // legacy semantic AND consistent with shared() readers.
        DialogHistory shared = DialogHistory.shared();
        return shared == null ? conversationHistory : shared;
    }

    /** Returns reusable mutable progress variables for this save. */
    public ProgressTracker progress() {
        return progress;
    }

    /** Returns reusable mutable inventory quantities for this save. */
    public InventoryState inventory() {
        return inventory;
    }

    /** Returns reusable mutable wardrobe unlock and outfit state for this save. */
    public WardrobeState wardrobe() {
        return wardrobe;
    }

    /** Returns reusable mutable character states keyed by character id. */
    public Collection<CharacterState> characterStates() {
        return java.util.Collections.unmodifiableCollection(characterStates.values());
    }

    /** Adds or replaces a reusable mutable character state. */
    public void putCharacterState(CharacterState characterState) {
        CharacterState checkedState = Validation.requireNonNull(characterState, "Character state is required.");
        characterStates.put(checkedState.characterId(), checkedState);
    }

    /** Returns reusable mutable journal state for this save. */
    public JournalState journal() {
        return journal;
    }

    /** Returns reusable mutable character-location state for this save. */
    public LocationOccupancy locationOccupancy() {
        return locationOccupancy;
    }

    /** Registers an application-owned state section that participates in checkpoint rollback snapshots. */
    public void registerRollbackSnapshotSection(
            String sectionId,
            Supplier<SaveSnapshotSection> snapshotSupplier,
            Consumer<SaveSnapshotSection> restoreConsumer) {
        registerRollbackSnapshotSection(sectionId, snapshotSupplier, restoreConsumer, () -> {});
    }

    /**
     * Registers an application-owned state section with an explicit pre-load reset callback.
     *
     * <p>The reset is invoked by {@link #restore(GameplayStateSnapshot)} on EVERY registered
     * section — including ones that aren't present in the loaded snapshot — BEFORE the
     * restore consumers fire.  This guarantees that loading a save fully overwrites the
     * current in-memory state instead of merging into it:
     * <ul>
     *   <li>For sections that ARE in the loaded snapshot, reset wipes them clean, then
     *       the restore consumer applies the payload.  Idempotent with consumer-side
     *       {@code .clear()} that hosts already commonly do — calling both is harmless.</li>
     *   <li>For sections that are NOT in the loaded snapshot (rare — happens only when
     *       the host's section set has changed across builds, or a partial save was
     *       written), reset wipes them clean and they stay empty.  Without this, those
     *       sections would silently retain their pre-load (boot-default or previous-
     *       load) state and bleed into the new game.</li>
     * </ul></p>
     *
     * <p>Pass a Runnable that resets the section's in-memory state to its empty/fresh
     * baseline — typically {@code XClass::clear}.  Hosts registering via the 3-arg
     * overload above get a no-op reset for back-compat; they remain responsible for
     * consumer-side clearing.</p>
     */
    public void registerRollbackSnapshotSection(
            String sectionId,
            Supplier<SaveSnapshotSection> snapshotSupplier,
            Consumer<SaveSnapshotSection> restoreConsumer,
            Runnable resetCallback) {
        CustomRollbackSnapshotSection section = new CustomRollbackSnapshotSection(
                sectionId,
                snapshotSupplier,
                restoreConsumer,
                resetCallback);
        customRollbackSections.put(section.sectionId(), section);
    }

    /** Captures all reusable mutable gameplay state with the current game-clock value. */
    public GameplayStateSnapshot snapshot(GameClock gameClock) {
        return snapshot(Validation.requireNonNull(gameClock, "Game clock is required.").currentTime());
    }

    /** Captures all reusable mutable gameplay state with an explicit game-clock value. */
    public GameplayStateSnapshot snapshot(GameDateTime gameTime) {
        return new GameplayStateSnapshot(
                gameTime,
                progress.snapshot(),
                InventorySnapshot.fromState(inventory),
                WardrobeSnapshot.fromState(wardrobe),
                CharacterStatesSnapshot.fromStates(characterStates.values()),
                JournalSnapshot.fromState(journal),
                LocationOccupancySnapshot.fromState(locationOccupancy),
                captureCustomSections());
    }

    /** Captures every registered custom rollback section, isolating failures: a section whose
     *  supplier throws is LOGGED and OMITTED rather than aborting the whole snapshot.  Without this,
     *  one misbehaving section's supplier would make {@code gameState.snapshot()} throw — and a save
     *  capture that throws writes NO snapshot file at all (the slot ends up route-only and won't
     *  load), so a single bad section would silently destroy the entire save. */
    private java.util.List<SaveSnapshotSection> captureCustomSections() {
        java.util.List<SaveSnapshotSection> sections = new java.util.ArrayList<>();
        for (CustomRollbackSnapshotSection section : customRollbackSections.values()) {
            try {
                sections.add(section.snapshot());
            } catch (RuntimeException ex) {
                System.err.println("[GameState] Snapshot supplier threw for section "
                        + section.sectionId() + " — omitting it from this snapshot (its state will"
                        + " not persist this time, but the rest of the save is written). " + ex);
            }
        }
        return sections;
    }

    /** Restores reusable mutable gameplay state and game time from a checkpoint/save snapshot. */
    public void restore(GameplayStateSnapshot snapshot, GameClock gameClock) {
        GameplayStateSnapshot checkedSnapshot =
                Validation.requireNonNull(snapshot, "Gameplay state snapshot is required.");
        Validation.requireNonNull(gameClock, "Game clock is required.").setCurrentTime(checkedSnapshot.gameTime());
        restore(checkedSnapshot);
    }

    /** Restores reusable mutable gameplay state from a checkpoint/save snapshot.  Built-in
     *  fields (progress, inventory, wardrobe, character states, journal, location
     *  occupancy) are wholesale-replaced — the snapshot payload defines them entirely.
     *  Custom rollback sections are RESET FIRST (every registered section, regardless of
     *  whether it appears in the loaded snapshot), then restored from the snapshot's
     *  per-section payloads.  This guarantees current game state is fully cleared before
     *  the loaded data is applied; see {@link #registerRollbackSnapshotSection(String,
     *  Supplier, Consumer, Runnable) the 4-arg register overload} for the reset
     *  contract. */
    public void restore(GameplayStateSnapshot snapshot) {
        GameplayStateSnapshot checkedSnapshot =
                Validation.requireNonNull(snapshot, "Gameplay state snapshot is required.");
        progress = new ProgressTracker();
        progress.restore(checkedSnapshot.progress());
        inventory = checkedSnapshot.inventory().toState();
        wardrobe = checkedSnapshot.wardrobe().toState();
        characterStates.clear();
        checkedSnapshot.characters().toStates().forEach(this::putCharacterState);
        journal = checkedSnapshot.journal().toState();
        locationOccupancy = checkedSnapshot.locationOccupancy().toState();
        // Reset EVERY registered custom rollback section to its empty baseline BEFORE
        // applying the loaded payloads.  Sections present in the snapshot will be
        // overwritten by their restore-consumer (so the reset is harmless duplicate
        // work).  Sections absent from the snapshot stay in their reset state — without
        // this pass they'd silently retain whatever was in memory from a previous load
        // or the boot default, bleeding into the loaded game.
        for (CustomRollbackSnapshotSection section : customRollbackSections.values()) {
            try {
                section.resetCallback().run();
            } catch (RuntimeException ex) {
                System.err.println("[GameState] Reset callback threw for section "
                        + section.sectionId() + " — load proceeds, but this section may"
                        + " carry stale state if it isn't overwritten by the snapshot. " + ex);
            }
        }
        checkedSnapshot.customSections().forEach(section -> {
            CustomRollbackSnapshotSection customSection = customRollbackSections.get(section.sectionId());
            if (customSection != null) {
                try {
                    customSection.restore(section);
                } catch (RuntimeException ex) {
                    // Isolate restore failures the same way the reset pass does — one section's
                    // consumer throwing must not abort the entire load.
                    System.err.println("[GameState] Restore consumer threw for section "
                            + section.sectionId() + " — load proceeds without this section. " + ex);
                }
            }
        });
    }

    private record CustomRollbackSnapshotSection(
            String sectionId,
            Supplier<SaveSnapshotSection> snapshotSupplier,
            Consumer<SaveSnapshotSection> restoreConsumer,
            Runnable resetCallback) {
        private CustomRollbackSnapshotSection {
            sectionId = Validation.requireNonBlank(sectionId, "Custom rollback snapshot section id is required.");
            snapshotSupplier = Validation.requireNonNull(snapshotSupplier, "Custom rollback snapshot supplier is required.");
            restoreConsumer = Validation.requireNonNull(restoreConsumer, "Custom rollback restore consumer is required.");
            resetCallback = Validation.requireNonNull(resetCallback, "Custom rollback reset callback is required.");
        }

        private SaveSnapshotSection snapshot() {
            SaveSnapshotSection section =
                    Validation.requireNonNull(snapshotSupplier.get(), "Custom rollback snapshot section is required.");
            if (!sectionId.equals(section.sectionId())) {
                throw new IllegalStateException("Custom rollback snapshot section id mismatch: " + section.sectionId());
            }
            return section;
        }

        private void restore(SaveSnapshotSection section) {
            restoreConsumer.accept(section);
        }
    }
}
