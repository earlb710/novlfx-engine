package com.eb.javafx.characters;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Registry for generic character profiles used by scenes, displays, and diagnostics. */
public final class CharacterRegistry {
    private final Map<String, CharacterProfile> profiles = new LinkedHashMap<>();

    public void register(CharacterProfile profile) {
        CharacterProfile checkedProfile = Validation.requireNonNull(profile, "Character profile is required.");
        if (profiles.containsKey(checkedProfile.id())) {
            throw new IllegalArgumentException("Character already registered: " + checkedProfile.id());
        }
        profiles.put(checkedProfile.id(), checkedProfile);
    }

    public Optional<CharacterProfile> profile(String characterId) {
        return Optional.ofNullable(profiles.get(characterId));
    }

    public java.util.List<CharacterProfile> profiles() {
        return Collections.unmodifiableList(new ArrayList<>(profiles.values()));
    }
}
