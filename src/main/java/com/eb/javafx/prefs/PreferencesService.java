package com.eb.javafx.prefs;

import com.eb.javafx.gamesupport.SystemCodeTables;

import java.util.prefs.Preferences;

/**
 * Section 1.8 JavaFX service for persistent preferences.
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
    private static final String FOOTER_LABELS_VISIBLE_KEY = "ui.footerLabelsVisible";
    private static final String FOOTER_SHORTCUT_DISPLAY_KEY = "ui.footerShortcutDisplay";
    private static final String FOOTER_ICON_DISPLAY_KEY = "ui.footerIconDisplay";
    private static final String FONT_FAMILY_KEY = "ui.fontFamily";
    private static final String FONT_SCALE_KEY = "ui.fontScale";
    private static final String THEME_FAMILY_KEY = "ui.themeFamily";
    private static final String THEME_VARIANT_KEY = "ui.themeVariant";
    private static final String HIGH_CONTRAST_KEY = "accessibility.highContrast";
    private static final String REDUCED_MOTION_KEY = "accessibility.reducedMotion";
    private static final String INPUT_MODE_KEY = "input.mode";
    private static final String MASTER_VOLUME_KEY = "audio.masterVolume";
    private static final String MUSIC_VOLUME_KEY = "audio.musicVolume";
    private static final String SOUND_VOLUME_KEY = "audio.soundVolume";
    private static final String MUTE_ALL_KEY = "audio.muteAll";
    private static final String VOICE_VOLUME_KEY = "audio.voiceVolume";
    private static final String VOICE_ENABLED_KEY = "audio.voiceEnabled";
    private static final String AUTO_ADVANCE_ON_VOICE_END_KEY = "audio.autoAdvanceOnVoiceEnd";
    private static final String FULLSCREEN_KEY = "window.fullscreen";
    private static final String LANGUAGE_KEY = "ui.language";

    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);
    private int windowWidth;
    private int windowHeight;
    private double hudAlpha;
    private double sayWindowAlpha;
    private boolean showPortrait;
    private boolean cheatsVisible;
    private boolean logStatChanges;
    private FooterShortcutDisplay footerShortcutDisplay;
    private FooterIconDisplay footerIconDisplay;
    private String fontFamily;
    private double fontScale;
    private ThemeFamily themeFamily;
    private ThemeVariant themeVariant;
    private boolean highContrast;
    private boolean reducedMotion;
    private String inputMode;
    private double masterVolume;
    private double musicVolume;
    private double soundVolume;
    private boolean muteAll;
    private double voiceVolume;
    private boolean voiceEnabled;
    private boolean autoAdvanceOnVoiceEnd;
    private boolean fullscreen;
    private Language language;

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
        footerShortcutDisplay = validatedFooterShortcutDisplay(footerShortcutDisplayPreferenceValue());
        footerIconDisplay = validatedFooterIconDisplay(
                preferences.get(FOOTER_ICON_DISPLAY_KEY, FooterIconDisplay.ICONS_WITH_TEXT.preferenceValue()));
        fontFamily = preferences.get(FONT_FAMILY_KEY, "System");
        fontScale = clamp(preferences.getDouble(FONT_SCALE_KEY, 1.0), 0.75, 2.0);
        themeFamily = validatedThemeFamily(preferences.get(THEME_FAMILY_KEY, ThemeFamily.OCEAN.preferenceValue()));
        themeVariant = validatedThemeVariant(preferences.get(THEME_VARIANT_KEY, ThemeVariant.DARK.preferenceValue()));
        highContrast = preferences.getBoolean(HIGH_CONTRAST_KEY, false);
        reducedMotion = preferences.getBoolean(REDUCED_MOTION_KEY, false);
        inputMode = validatedInputMode(preferences.get(INPUT_MODE_KEY, "mouse"));
        masterVolume = clamp(preferences.getDouble(MASTER_VOLUME_KEY, 1.0), 0.0, 1.0);
        musicVolume = clamp(preferences.getDouble(MUSIC_VOLUME_KEY, 1.0), 0.0, 1.0);
        soundVolume = clamp(preferences.getDouble(SOUND_VOLUME_KEY, 1.0), 0.0, 1.0);
        muteAll = preferences.getBoolean(MUTE_ALL_KEY, false);
        voiceVolume = clamp(preferences.getDouble(VOICE_VOLUME_KEY, 1.0), 0.0, 1.0);
        voiceEnabled = preferences.getBoolean(VOICE_ENABLED_KEY, true);
        autoAdvanceOnVoiceEnd = preferences.getBoolean(AUTO_ADVANCE_ON_VOICE_END_KEY, false);
        fullscreen = preferences.getBoolean(FULLSCREEN_KEY, false);
        language = validatedLanguage(preferences.get(LANGUAGE_KEY, Language.ENGLISH.preferenceValue()));
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

    /** Returns whether footer buttons should show labels and shortcuts in addition to icons. */
    public boolean footerLabelsVisible() {
        return footerShortcutDisplay == FooterShortcutDisplay.DISPLAY;
    }

    /** Returns how footer keyboard shortcuts should be displayed. */
    public FooterShortcutDisplay footerShortcutDisplay() {
        return footerShortcutDisplay;
    }

    /** Returns how footer button icons should be displayed relative to their text labels. */
    public FooterIconDisplay footerIconDisplay() {
        return footerIconDisplay;
    }

    /** Returns the preferred JavaFX font family for migrated UI controls. */
    public String fontFamily() {
        return fontFamily;
    }

    /** Returns the early startup font scale for accessible JavaFX layouts. */
    public double fontScale() {
        return fontScale;
    }

    /** Returns the preferred UI theme family. */
    public ThemeFamily themeFamily() {
        return themeFamily;
    }

    /** Returns the preferred UI theme variant. */
    public ThemeVariant themeVariant() {
        return themeVariant;
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

    /** Returns the persisted music channel volume multiplier. */
    public double musicVolume() {
        return musicVolume;
    }

    /** Returns the persisted sound channel volume multiplier. */
    public double soundVolume() {
        return soundVolume;
    }

    /** Returns whether all audio channels should be muted. */
    public boolean muteAll() {
        return muteAll;
    }

    /** Returns the persisted voice channel volume multiplier. */
    public double voiceVolume() {
        return voiceVolume;
    }

    /** Returns whether the voice channel is enabled. */
    public boolean voiceEnabled() {
        return voiceEnabled;
    }

    /** Returns whether scene steps should auto-advance when voice playback ends. */
    public boolean autoAdvanceOnVoiceEnd() {
        return autoAdvanceOnVoiceEnd;
    }

    /** Returns whether the primary window should be displayed in fullscreen mode. */
    public boolean fullscreen() {
        return fullscreen;
    }

    /** Returns the preferred UI language. */
    public Language language() {
        return language;
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

    /**
     * Persists the legacy footer label preference and mirrors it to the current shortcut display mode.
     *
     * <p>The legacy key is retained so older callers that still save this boolean do not lose their selected
     * behavior while newer code reads {@link #footerShortcutDisplay()}.</p>
     */
    public void saveFooterLabelsVisible(boolean footerLabelsVisible) {
        preferences.putBoolean(FOOTER_LABELS_VISIBLE_KEY, footerLabelsVisible);
        saveFooterShortcutDisplay(footerLabelsVisible ? FooterShortcutDisplay.DISPLAY : FooterShortcutDisplay.HIDE);
    }

    /** Persists how reusable footer buttons should show keyboard shortcuts. */
    public void saveFooterShortcutDisplay(FooterShortcutDisplay footerShortcutDisplay) {
        this.footerShortcutDisplay = footerShortcutDisplay == null
                ? FooterShortcutDisplay.TOOLTIP_ONLY
                : footerShortcutDisplay;
        preferences.put(FOOTER_SHORTCUT_DISPLAY_KEY, this.footerShortcutDisplay.preferenceValue());
    }

    /** Persists a validated footer shortcut display value, falling back to tooltip-only for unknown strings. */
    public void saveFooterShortcutDisplay(String footerShortcutDisplay) {
        saveFooterShortcutDisplay(validatedFooterShortcutDisplay(footerShortcutDisplay));
    }

    /** Persists how reusable footer buttons should display icons relative to text labels. */
    public void saveFooterIconDisplay(FooterIconDisplay footerIconDisplay) {
        this.footerIconDisplay = footerIconDisplay == null
                ? FooterIconDisplay.ICONS_WITH_TEXT
                : footerIconDisplay;
        preferences.put(FOOTER_ICON_DISPLAY_KEY, this.footerIconDisplay.preferenceValue());
    }

    /** Persists a validated footer icon display value, falling back to icons-with-text for unknown strings. */
    public void saveFooterIconDisplay(String footerIconDisplay) {
        saveFooterIconDisplay(validatedFooterIconDisplay(footerIconDisplay));
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

    /** Persists the selected theme family and variant. */
    public void saveThemePreferences(ThemeFamily themeFamily, ThemeVariant themeVariant) {
        this.themeFamily = themeFamily == null ? ThemeFamily.OCEAN : themeFamily;
        this.themeVariant = themeVariant == null ? ThemeVariant.DARK : themeVariant;
        preferences.put(THEME_FAMILY_KEY, this.themeFamily.preferenceValue());
        preferences.put(THEME_VARIANT_KEY, this.themeVariant.preferenceValue());
    }

    /** Persists validated theme identifiers, falling back to ocean/dark for unknown values. */
    public void saveThemePreferences(String themeFamily, String themeVariant) {
        saveThemePreferences(validatedThemeFamily(themeFamily), validatedThemeVariant(themeVariant));
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

    /** Persists clamped music and sound channel volumes for the default preferences screen. */
    public void saveAudioChannelVolumes(double musicVolume, double soundVolume) {
        this.musicVolume = clamp(musicVolume, 0.0, 1.0);
        this.soundVolume = clamp(soundVolume, 0.0, 1.0);
        preferences.putDouble(MUSIC_VOLUME_KEY, this.musicVolume);
        preferences.putDouble(SOUND_VOLUME_KEY, this.soundVolume);
    }

    /** Persists a clamped music channel volume. */
    public void saveMusicVolume(double musicVolume) {
        this.musicVolume = clamp(musicVolume, 0.0, 1.0);
        preferences.putDouble(MUSIC_VOLUME_KEY, this.musicVolume);
    }

    /** Persists a clamped sound channel volume. */
    public void saveSoundVolume(double soundVolume) {
        this.soundVolume = clamp(soundVolume, 0.0, 1.0);
        preferences.putDouble(SOUND_VOLUME_KEY, this.soundVolume);
    }

    /** Persists the mute-all preference and updates the loaded value. */
    public void saveMuteAll(boolean muteAll) {
        this.muteAll = muteAll;
        preferences.putBoolean(MUTE_ALL_KEY, muteAll);
    }

    /** Persists a clamped voice channel volume. */
    public void saveVoiceVolume(double voiceVolume) {
        this.voiceVolume = clamp(voiceVolume, 0.0, 1.0);
        preferences.putDouble(VOICE_VOLUME_KEY, this.voiceVolume);
    }

    /** Persists whether the voice channel is enabled. */
    public void saveVoiceEnabled(boolean voiceEnabled) {
        this.voiceEnabled = voiceEnabled;
        preferences.putBoolean(VOICE_ENABLED_KEY, voiceEnabled);
    }

    /** Persists the auto-advance-on-voice-end preference. */
    public void saveAutoAdvanceOnVoiceEnd(boolean autoAdvanceOnVoiceEnd) {
        this.autoAdvanceOnVoiceEnd = autoAdvanceOnVoiceEnd;
        preferences.putBoolean(AUTO_ADVANCE_ON_VOICE_END_KEY, autoAdvanceOnVoiceEnd);
    }

    /** Persists the fullscreen preference and updates the loaded value. */
    public void saveFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        preferences.putBoolean(FULLSCREEN_KEY, fullscreen);
    }

    /** Persists the selected UI language. */
    public void saveLanguage(Language language) {
        this.language = language == null ? Language.ENGLISH : language;
        preferences.put(LANGUAGE_KEY, this.language.preferenceValue());
    }

    /** Persists a validated language identifier, falling back to English for unknown values. */
    public void saveLanguage(String language) {
        saveLanguage(validatedLanguage(language));
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

    private ThemeFamily validatedThemeFamily(String value) {
        for (ThemeFamily family : ThemeFamily.values()) {
            if (family.preferenceValue().equals(value)) {
                return family;
            }
        }
        return ThemeFamily.OCEAN;
    }

    private ThemeVariant validatedThemeVariant(String value) {
        for (ThemeVariant variant : ThemeVariant.values()) {
            if (variant.preferenceValue().equals(value)) {
                return variant;
            }
        }
        return ThemeVariant.DARK;
    }

    private String footerShortcutDisplayPreferenceValue() {
        String configuredValue = preferences.get(FOOTER_SHORTCUT_DISPLAY_KEY, null);
        if (configuredValue != null) {
            return configuredValue;
        }
        String legacyLabelsVisible = preferences.get(FOOTER_LABELS_VISIBLE_KEY, null);
        if (legacyLabelsVisible != null) {
            return Boolean.parseBoolean(legacyLabelsVisible)
                    ? FooterShortcutDisplay.DISPLAY.preferenceValue()
                    : FooterShortcutDisplay.HIDE.preferenceValue();
        }
        return FooterShortcutDisplay.TOOLTIP_ONLY.preferenceValue();
    }

    private Language validatedLanguage(String value) {
        for (Language candidate : Language.values()) {
            if (candidate.preferenceValue().equals(value)) {
                return candidate;
            }
        }
        return Language.ENGLISH;
    }

    private FooterShortcutDisplay validatedFooterShortcutDisplay(String value) {
        for (FooterShortcutDisplay display : FooterShortcutDisplay.values()) {
            if (display.preferenceValue().equals(value)) {
                return display;
            }
        }
        return FooterShortcutDisplay.TOOLTIP_ONLY;
    }

    private FooterIconDisplay validatedFooterIconDisplay(String value) {
        for (FooterIconDisplay display : FooterIconDisplay.values()) {
            if (display.preferenceValue().equals(value)) {
                return display;
            }
        }
        return FooterIconDisplay.ICONS_WITH_TEXT;
    }

    /** User preference for whether footer shortcut text is visible, hidden, or represented by tooltips only. */
    public enum FooterShortcutDisplay {
        DISPLAY("display"),
        HIDE("hide"),
        TOOLTIP_ONLY("tooltip-only");

        private final String preferenceValue;

        FooterShortcutDisplay(String preferenceValue) {
            this.preferenceValue = preferenceValue;
        }

        public String preferenceValue() {
            return preferenceValue;
        }

        public String label() {
            return SystemCodeTables.defaultCodeTitle(SystemCodeTables.FOOTER_SHORTCUT_DISPLAY_TABLE_ID, preferenceValue);
        }
    }

    /** User preference for whether footer buttons show icons, text, or both. */
    public enum FooterIconDisplay {
        ICONS_ONLY("icons-only"),
        ICONS_WITH_TEXT("icons-with-text"),
        TEXT_ONLY("text-only");

        private final String preferenceValue;

        FooterIconDisplay(String preferenceValue) {
            this.preferenceValue = preferenceValue;
        }

        public String preferenceValue() {
            return preferenceValue;
        }

        public String label() {
            return SystemCodeTables.defaultCodeTitle(SystemCodeTables.FOOTER_ICON_DISPLAY_TABLE_ID, preferenceValue);
        }
    }

    /** Supported UI theme color families persisted as user preference values. */
    public enum ThemeFamily {
        OCEAN("ocean"),
        FOREST("forest"),
        SUNSET("sunset"),
        VIOLET("violet"),
        CRIMSON("crimson");

        private final String preferenceValue;

        ThemeFamily(String preferenceValue) {
            this.preferenceValue = preferenceValue;
        }

        public String preferenceValue() {
            return preferenceValue;
        }

        public String label() {
            return SystemCodeTables.defaultCodeTitle(SystemCodeTables.THEME_FAMILY_TABLE_ID, preferenceValue);
        }
    }

    /** Supported UI languages persisted as user preference values. Only English is fully supported today. */
    public enum Language {
        ENGLISH("en", "English", true),
        SPANISH("es", "Español", false),
        FRENCH("fr", "Français", false),
        JAPANESE("ja", "日本語", false);

        private final String preferenceValue;
        private final String label;
        private final boolean enabled;

        Language(String preferenceValue, String label, boolean enabled) {
            this.preferenceValue = preferenceValue;
            this.label = label;
            this.enabled = enabled;
        }

        public String preferenceValue() {
            return preferenceValue;
        }

        public String label() {
            return label;
        }

        public boolean enabled() {
            return enabled;
        }
    }

    /** Supported UI theme brightness variants persisted as user preference values. */
    public enum ThemeVariant {
        DARK("dark"),
        LIGHT_PASTEL("light-pastel");

        private final String preferenceValue;

        ThemeVariant(String preferenceValue) {
            this.preferenceValue = preferenceValue;
        }

        public String preferenceValue() {
            return preferenceValue;
        }

        public String label() {
            return SystemCodeTables.defaultCodeTitle(SystemCodeTables.THEME_VARIANT_TABLE_ID, preferenceValue);
        }
    }
}
