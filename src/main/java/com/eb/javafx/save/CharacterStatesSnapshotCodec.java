package com.eb.javafx.save;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Save snapshot codec for generic character state. */
public final class CharacterStatesSnapshotCodec implements SaveSnapshotCodec<CharacterStatesSnapshot> {
    public static final String SECTION_ID = "characters";
    public static final int SCHEMA_VERSION = 1;

    @Override
    public String sectionId() {
        return SECTION_ID;
    }

    @Override
    public int schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public String toJson(CharacterStatesSnapshot snapshot) {
        CharacterStatesSnapshot checkedSnapshot = Validation.requireNonNull(snapshot, "Character states snapshot is required.");
        return "{\n"
                + "  \"characters\": " + checkedSnapshot.characters().stream()
                .map(CharacterStatesSnapshotCodec::characterJson)
                .collect(Collectors.joining(", ", "[", "]"))
                + "\n"
                + "}";
    }

    @Override
    public CharacterStatesSnapshot fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<CharacterSnapshot> characters = JsonData.optionalList(root, "characters", "characters").stream()
                .map(entry -> characterSnapshot(JsonData.requireObject(entry, "character snapshot")))
                .toList();
        return new CharacterStatesSnapshot(characters);
    }

    private static String characterJson(CharacterSnapshot character) {
        return "{"
                + "\"characterId\": " + JsonStrings.quote(character.characterId()) + ", "
                + "\"templateId\": " + JsonStrings.quote(character.templateId()) + ", "
                + "\"stats\": " + SnapshotJson.integerMap(character.stats()) + ", "
                + "\"relationships\": " + SnapshotJson.integerMap(character.relationships()) + ", "
                + "\"flags\": " + SnapshotJson.stringArray(character.flags()) + ", "
                + "\"metadata\": " + SnapshotJson.stringMap(character.metadata())
                + "}";
    }

    private static CharacterSnapshot characterSnapshot(Map<String, Object> object) {
        return new CharacterSnapshot(
                JsonData.requiredString(object, "characterId", "character id"),
                JsonData.requiredString(object, "templateId", "character template id"),
                SnapshotJson.integerMap(object.getOrDefault("stats", Map.of()), "character stats"),
                SnapshotJson.integerMap(object.getOrDefault("relationships", Map.of()), "character relationships"),
                SnapshotJson.stringSet(object.getOrDefault("flags", List.of()), "character flags"),
                SnapshotJson.stringMap(object.getOrDefault("metadata", Map.of()), "character metadata"));
    }
}
