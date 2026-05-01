package com.eb.javafx.prefs;

import java.util.prefs.Preferences;

/**
 * Section 1.8 JavaFX replacement for Ren'Py persistent preferences.
 *
 * <p>The service deliberately stores display preferences outside save data. That
 * mirrors the plan's distinction between persistent user settings and serialized
 * game state, and it lets the JavaFX window be sized before the first scene is
 * created.</p>
 */
public final class PreferencesService {
    private static final String WIDTH_KEY = "window.width";
    private static final String HEIGHT_KEY = "window.height";
    private static final String HUD_ALPHA_KEY = "ui.hudAlpha";
    private static final String SAY_WINDOW_ALPHA_KEY = "ui.sayWindowAlpha";
    private static final String SHOW_PORTRAIT_KEY = "ui.showPortrait";
    private static final String CHEATS_VISIBLE_KEY = "ui.cheatsVisible";
    private static final String LOG_STAT_CHANGES_KEY = "ui.logStatChanges";
    private static final String FONT_FAMILY_KEY = "ui.fontFamily";
    private static final String FONT_SCALE_KEY = "ui.fontScale";
    private static final String HIGH_CONTRAST_KEY = "accessibility.highContrast";
    private static final String REDUCED_MOTION_KEY = "accessibility.reducedMotion";
    private static final String INPUT_MODE_KEY = "input.mode";
    private static final String MASTER_VOLUME_KEY = "audio.masterVolume";

    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);
    private int windowWidth;
    private int windowHeight;
    private double hudAlpha;
    private double sayWindowAlpha;
    private boolean showPortrait;
    private boolean cheatsVisible;
    private boolean logStatChanges;
    private String fontFamily;
    private double fontScale;
    private boolean highContrast;
    private boolean reducedMotion;
    private String inputMode;
    private double masterVolume;

    /**
     * Loads startup preferences with conservative defaults for the first shell.
     *
     * <p>Values are read from Java user preferences and clamped or normalized before
     * callers consume them during bootstrap.</p>
     */
    public void load() {
        windowWidth = clamp(preferences.getInt(WIDTH_KEY, 1280), 640, 3840);
        windowHeight = clamp(preferences.getInt(HEIGHT_KEY, 720), 480, 2160);
        hudAlpha = clamp(preferences.getDouble(HUD_ALPHA_KEY, 1.0), 0.0, 1.0);
        sayWindowAlpha = clamp(preferences.getDouble(SAY_WINDOW_ALPHA_KEY, 1.0), 0.0, 1.0);
        showPortrait = preferences.getBoolean(SHOW_PORTRAIT_KEY, true);
        cheatsVisible = preferences.getBoolean(CHEATS_VISIBLE_KEY, true);
        logStatChanges = preferences.getBoolean(LOG_STAT_CHANGES_KEY, false);
        fontFamily = preferences.get(FONT_FAMILY_KEY, "System");
        fontScale = clamp(preferences.getDouble(FONT_SCALE_KEY, 1.0), 0.75, 2.0);
        highContrast = preferences.getBoolean(HIGH_CONTRAST_KEY, false);
        reducedMotion = preferences.getBoolean(REDUCED_MOTION_KEY, false);
        inputMode = validatedInputMode(preferences.get(INPUT_MODE_KEY, "mouse"));
        masterVolume = clamp(preferences.getDouble(MASTER_VOLUME_KEY, 1.0), 0.0, 1.0);
    }

    /** Returns the configured starting width for JavaFX scenes. */
    public int windowWidth() {
        return windowWidth;
    }

    /** Returns the configured starting height for JavaFX scenes. */
    public int windowHeight() {
        return windowHeight;
    }

    /** Returns the window title used by the primary stage. */
    public String windowTitle() {
        return "eb JavaFX";
    }

    /** Returns the configured HUD opacity that replaces persistent.hud_alpha. */
    public double hudAlpha() {
        return hudAlpha;
    }

    /** Returns the configured dialogue box opacity that replaces persistent.say_window_alpha. */
    public double sayWindowAlpha() {
        return sayWindowAlpha;
    }

    /** Returns whether dialogue portraits should be shown by default. */
    public boolean showPortrait() {
        return showPortrait;
    }

    /** Returns whether cheat/debug UI should be visible. */
    public boolean cheatsVisible() {
        return cheatsVisible;
    }

    /** Returns whether stat changes should be logged in the UI. */
    public boolean logStatChanges() {
        return logStatChanges;
    }

    /** Returns the preferred JavaFX font family for migrated UI controls. */
    public String fontFamily() {
        return fontFamily;
    }

    /** Returns the early startup font scale for accessible JavaFX layouts. */
    public double fontScale() {
        return fontScale;
    }

    /** Returns whether high-contrast JavaFX styling should be preferred. */
    public boolean highContrast() {
        return highContrast;
    }

    /** Returns whether migrated UI should avoid non-essential animation. */
    public boolean reducedMotion() {
        return reducedMotion;
    }

    /** Returns the preferred initial input mode for focus and shortcut behavior. */
    public String inputMode() {
        return inputMode;
    }

    /** Returns the early startup master volume for future audio service wiring. */
    public double masterVolume() {
        return masterVolume;
    }

    /** Persists a clamped window size separately from save-game state. */
    public void saveWindowSize(double width, double height) {
        preferences.putInt(WIDTH_KEY, clamp((int) Math.round(width), 640, 3840));
        preferences.putInt(HEIGHT_KEY, clamp((int) Math.round(height), 480, 2160));
    }

    /** Persists UI visibility preferences and updates the loaded model. */
    public void saveUiVisibility(boolean showPortrait, boolean cheatsVisible, boolean logStatChanges) {
        preferences.putBoolean(SHOW_PORTRAIT_KEY, showPortrait);
        preferences.putBoolean(CHEATS_VISIBLE_KEY, cheatsVisible);
        preferences.putBoolean(LOG_STAT_CHANGES_KEY, logStatChanges);
        this.showPortrait = showPortrait;
        this.cheatsVisible = cheatsVisible;
        this.logStatChanges = logStatChanges;
    }

    /** Persists dialogue/HUD opacity preferences and updates the loaded model. */
    public void saveUiOpacity(double hudAlpha, double sayWindowAlpha) {
        this.hudAlpha = clamp(hudAlpha, 0.0, 1.0);
        this.sayWindowAlpha = clamp(sayWindowAlpha, 0.0, 1.0);
        preferences.putDouble(HUD_ALPHA_KEY, this.hudAlpha);
        preferences.putDouble(SAY_WINDOW_ALPHA_KEY, this.sayWindowAlpha);
    }

    /**
     * Persists font preferences and updates the loaded model.
     *
     * <p>Blank font families fall back to {@code System}; scale is clamped to the
     * accessible startup range.</p>
     */
    public void saveFontPreferences(String fontFamily, double fontScale) {
        this.fontFamily = fontFamily == null || fontFamily.isBlank() ? "System" : fontFamily;
        this.fontScale = clamp(fontScale, 0.75, 2.0);
        preferences.put(FONT_FAMILY_KEY, this.fontFamily);
        preferences.putDouble(FONT_SCALE_KEY, this.fontScale);
    }

    /** Persists accessibility preferences and updates the loaded model. */
    public void saveAccessibilityPreferences(boolean highContrast, boolean reducedMotion) {
        preferences.putBoolean(HIGH_CONTRAST_KEY, highContrast);
        preferences.putBoolean(REDUCED_MOTION_KEY, reducedMotion);
        this.highContrast = highContrast;
        this.reducedMotion = reducedMotion;
    }

    /** Persists the preferred input mode, accepting only keyboard, mouse, or touch. */
    public void saveInputMode(String inputMode) {
        this.inputMode = validatedInputMode(inputMode);
        preferences.put(INPUT_MODE_KEY, this.inputMode);
    }

    /** Persists clamped master volume for section 1.5 audio service startup. */
    public void saveMasterVolume(double masterVolume) {
        this.masterVolume = clamp(masterVolume, 0.0, 1.0);
        preferences.putDouble(MASTER_VOLUME_KEY, this.masterVolume);
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private String validatedInputMode(String value) {
        if ("keyboard".equals(value) || "mouse".equals(value) || "touch".equals(value)) {
            return value;
        }
        return "mouse";
    }
}
