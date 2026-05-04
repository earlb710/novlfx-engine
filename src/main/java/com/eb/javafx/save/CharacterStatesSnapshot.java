package com.eb.javafx.save;

import com.eb.javafx.characters.CharacterState;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.Collection;
import java.util.List;

/** Immutable save snapshot of reusable per-save character states. */
public record CharacterStatesSnapshot(List<CharacterSnapshot> characters) {
    public CharacterStatesSnapshot {
        characters = ImmutableCollections.copyList(characters);
    }

    public static CharacterStatesSnapshot empty() {
        return new CharacterStatesSnapshot(List.of());
    }

    public static CharacterStatesSnapshot fromStates(Collection<CharacterState> states) {
        Validation.requireNonNull(states, "Character states are required.");
        return new CharacterStatesSnapshot(states.stream()
                .map(CharacterSnapshot::fromState)
                .toList());
    }

    public List<CharacterState> toStates() {
        return characters.stream()
                .map(CharacterSnapshot::toState)
                .toList();
    }
}
