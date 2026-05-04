package com.eb.javafx.settings;

import com.eb.javafx.util.Validation;

/** Definition for a player-facing game setting above raw preferences storage. */
public record SettingDefinition(String id, String title, SettingType type, String defaultValue) {
    public SettingDefinition {
        id = Validation.requireNonBlank(id, "Setting id is required.");
        title = Validation.requireNonBlank(title, "Setting title is required.");
        type = Validation.requireNonNull(type, "Setting type is required.");
        defaultValue = defaultValue == null ? "" : defaultValue;
    }
}
