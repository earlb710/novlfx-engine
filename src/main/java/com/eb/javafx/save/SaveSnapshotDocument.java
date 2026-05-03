package com.eb.javafx.save;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ordered multi-section snapshot document for composing engine-owned save slices.
 */
public final class SaveSnapshotDocument {
    private final List<SaveSnapshotSection> sections;

    public SaveSnapshotDocument(List<SaveSnapshotSection> sections) {
        this.sections = List.copyOf(Validation.requireNonNull(sections, "Save snapshot sections are required."));
    }

    public List<SaveSnapshotSection> sections() {
        return sections;
    }

    public Optional<SaveSnapshotSection> section(String sectionId) {
        Validation.requireNonBlank(sectionId, "Save snapshot section id is required.");
        return sections.stream().filter(section -> sectionId.equals(section.sectionId())).findFirst();
    }

    public String toJson() {
        StringBuilder json = new StringBuilder("{\n  \"sections\": [\n");
        for (int index = 0; index < sections.size(); index++) {
            SaveSnapshotSection section = sections.get(index);
            json.append("    {")
                    .append("\"sectionId\": ").append(JsonStrings.quote(section.sectionId()))
                    .append(", \"schemaVersion\": ").append(section.schemaVersion())
                    .append(", \"payloadJson\": ").append(JsonStrings.quote(section.payloadJson()))
                    .append("}");
            if (index + 1 < sections.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n}\n");
        return json.toString();
    }

    public static SaveSnapshotDocument fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<SaveSnapshotSection> sections = JsonData.optionalList(root, "sections", "save snapshot sections").stream()
                .map(entry -> JsonData.requireObject(entry, "save snapshot section"))
                .map(object -> new SaveSnapshotSection(
                        JsonData.requiredString(object, "sectionId", "save snapshot section id"),
                        ((Number) Validation.requireNonNull(object.get("schemaVersion"), "Save snapshot section version is required.")).intValue(),
                        JsonData.requiredString(object, "payloadJson", "save snapshot payload JSON")))
                .toList();
        return new SaveSnapshotDocument(sections);
    }

    Map<String, SaveSnapshotSection> sectionMap() {
        Map<String, SaveSnapshotSection> values = new LinkedHashMap<>();
        for (SaveSnapshotSection section : sections) {
            values.put(section.sectionId(), section);
        }
        return values;
    }
}
