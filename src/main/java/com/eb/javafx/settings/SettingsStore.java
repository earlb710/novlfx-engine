package com.eb.javafx.settings;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Generic settings definition and value store independent of a concrete persistence backend. */
public final class SettingsStore {
    private final Map<String, SettingDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, String> values = new LinkedHashMap<>();

    public void register(SettingDefinition definition) {
        SettingDefinition checkedDefinition = Validation.requireNonNull(definition, "Setting definition is required.");
        if (definitions.containsKey(checkedDefinition.id())) {
            throw new IllegalArgumentException("Setting already registered: " + checkedDefinition.id());
        }
        definitions.put(checkedDefinition.id(), checkedDefinition);
    }

    public void set(String settingId, String value) {
        String checkedSettingId = Validation.requireNonBlank(settingId, "Setting id is required.");
        if (!definitions.containsKey(checkedSettingId)) {
            throw new IllegalArgumentException("Unknown setting: " + checkedSettingId);
        }
        values.put(checkedSettingId, value == null ? "" : value);
    }

    public Optional<SettingDefinition> definition(String settingId) {
        return Optional.ofNullable(definitions.get(settingId));
    }

    public String value(String settingId) {
        SettingDefinition definition = definition(settingId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown setting: " + settingId));
        return values.getOrDefault(settingId, definition.defaultValue());
    }
}
