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
import com.eb.javafx.save.WardrobeSnapshot;
import com.eb.javafx.text.DialogHistory;
import com.eb.javafx.util.Validation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

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

    /** Creates placeholder mutable state with the route selected by static content. */
    public GameState(String startupRoute) {
        this(startupRoute, new DialogHistory());
    }

    /** Creates placeholder mutable state with explicit reusable conversation history. */
    public GameState(String startupRoute, DialogHistory conversationHistory) {
        this.startupRoute = startupRoute;
        this.conversationHistory = Validation.requireNonNull(conversationHistory, "Conversation history is required.");
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
        return conversationHistory;
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
                LocationOccupancySnapshot.fromState(locationOccupancy));
    }

    /** Restores reusable mutable gameplay state and game time from a checkpoint/save snapshot. */
    public void restore(GameplayStateSnapshot snapshot, GameClock gameClock) {
        GameplayStateSnapshot checkedSnapshot =
                Validation.requireNonNull(snapshot, "Gameplay state snapshot is required.");
        Validation.requireNonNull(gameClock, "Game clock is required.").setCurrentTime(checkedSnapshot.gameTime());
        restore(checkedSnapshot);
    }

    /** Restores reusable mutable gameplay state from a checkpoint/save snapshot. */
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
    }
}
