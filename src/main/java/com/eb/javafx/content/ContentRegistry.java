package com.eb.javafx.content;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds static definitions loaded during application startup.
 *
 * <p>Ren'Py define variables and module-level lists currently create many static
 * game definitions as files are imported. This registry is the Java equivalent: it
 * gives those definitions an explicit home so migrated content can be loaded before
 * mutable save data is created.</p>
 */
public final class ContentRegistry {
    private final Map<String, String> definitions = new LinkedHashMap<>();

    /**
     * Registers the minimal placeholder content needed by the initial JavaFX shell.
     *
     * <p>Later migration work should replace these strings with parsed domain and
     * authored-content data supplied by app/game-specific content modules.</p>
     */
    public void registerBaseContent() {
        definitions.put("application.name", "Alternative World JavaFX Port");
        definitions.put("startup.route", "main-menu");
        definitions.put("migration.slice", "JAVAFX_PLAN 1.1 through 1.4 - lifecycle, screens, styles, and display bindings");
        definitions.put("ui.mainMenu.title", "Main Menu");
        definitions.put("ui.preferences.title", "Preferences");
        definitions.put("ui.saveLoad.title", "Save / Load");
        definitions.put("ui.dialogue.title", "Dialogue");
        definitions.put("ui.choice.title", "Choice Menu");
        definitions.put("ui.hud.title", "HUD");
        definitions.put("ui.notification.title", "Notification Overlay");
        definitions.put("ui.tooltip.title", "Tooltip");
        definitions.put("ui.displayBindings.title", "Display Bindings Preview");
        definitions.put("ui.captureTest.title", "Capture Test Screen");
    }

    /**
     * Validates content that must exist before game state or UI routes can use it.
     */
    public void validateRules() {
        requireDefinition("application.name");
        requireDefinition("startup.route");
        requireDefinition("migration.slice");
        requireDefinition("ui.mainMenu.title");
        requireDefinition("ui.preferences.title");
        requireDefinition("ui.saveLoad.title");
        requireDefinition("ui.dialogue.title");
        requireDefinition("ui.choice.title");
        requireDefinition("ui.hud.title");
        requireDefinition("ui.notification.title");
        requireDefinition("ui.tooltip.title");
        requireDefinition("ui.displayBindings.title");
        requireDefinition("ui.captureTest.title");
    }

    /** Returns an immutable view so controllers cannot mutate static startup data. */
    public Map<String, String> definitions() {
        return Collections.unmodifiableMap(definitions);
    }

    /** Retrieves a single definition by ID for UI labels or future service wiring. */
    public String definition(String id) {
        requireDefinition(id);
        return definitions.get(id);
    }

    private void requireDefinition(String id) {
        if (!definitions.containsKey(id)) {
            throw new IllegalStateException("Missing required content definition: " + id);
        }
    }
}
