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
    private static final String TEXT_SPEED_KEY = "ui.textSpeed";
    private static final String AUTO_SAVE_DAILY_KEY = "save.autoSaveDaily";
    private static final String SAVE_SCREEN_VIEW_MODE_KEY = "save.viewMode";
    private static final String SAVE_SCREEN_PAGE_COUNT_KEY = "save.pageCount";
    private static final String SAVE_SCREEN_SELECTED_PAGE_KEY = "save.selectedPage";

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
    private boolean autoSaveDaily;
    private SaveScreenViewMode saveScreenViewMode;
    private int saveScreenPageCount;
    private int saveScreenSelectedPage;
    private Language language;
    private TextSpeed textSpeed;

    // Config-overridable startup window sizing + clamp bounds (the `window` config object, applied
    // at boot before load()).  Defaults preserve the original 1280x720 within 640-3840 x 480-2160.
    private int defaultWindowWidth  = 1280;
    private int defaultWindowHeight = 720;
    private int minWindowWidth      = 640;
    private int maxWindowWidth      = 3840;
    private int minWindowHeight     = 480;
    private int maxWindowHeight     = 2160;
    // Config-overridable font-scale clamp range (the `ui.fontScaleMin/Max` fields).  Default 0.75-2.0.
    private double fontScaleMin = 0.75;
    private double fontScaleMax = 2.0;

    /** Overrides the startup window size and its clamp bounds (the {@code window} config object).
     *  Null args keep the current value; non-positive values are ignored; each max is clamped to be
     *  &ge; its min, and each default is clamped into its [min, max] range.  Call once before
     *  {@link #load()}. */
    public void setWindowSizeBounds(Integer defaultWidth, Integer defaultHeight,
                                    Integer minWidth, Integer maxWidth,
                                    Integer minHeight, Integer maxHeight) {
        if (minWidth != null && minWidth > 0)        { minWindowWidth = minWidth; }
        if (maxWidth != null && maxWidth > 0)        { maxWindowWidth = maxWidth; }
        if (minHeight != null && minHeight > 0)      { minWindowHeight = minHeight; }
        if (maxHeight != null && maxHeight > 0)      { maxWindowHeight = maxHeight; }
        if (maxWindowWidth < minWindowWidth)   { maxWindowWidth = minWindowWidth; }
        if (maxWindowHeight < minWindowHeight) { maxWindowHeight = minWindowHeight; }
        if (defaultWidth != null && defaultWidth > 0)   { defaultWindowWidth = defaultWidth; }
        if (defaultHeight != null && defaultHeight > 0) { defaultWindowHeight = defaultHeight; }
        defaultWindowWidth  = clamp(defaultWindowWidth, minWindowWidth, maxWindowWidth);
        defaultWindowHeight = clamp(defaultWindowHeight, minWindowHeight, maxWindowHeight);
    }

    /** Overrides the font-scale clamp range (the {@code ui.fontScaleMin/Max} fields).  Null args keep
     *  the current value; non-positive values are ignored; {@code max} is clamped to be &ge; {@code
     *  min}.  Call once before {@link #load()}. */
    public void setFontScaleBounds(Double min, Double max) {
        if (min != null && min > 0) { fontScaleMin = min; }
        if (max != null && max > 0) { fontScaleMax = max; }
        if (fontScaleMax < fontScaleMin) { fontScaleMax = fontScaleMin; }
    }

    /**
     * Loads startup preferences with conservative defaults for the first shell.
     *
     * <p>Values are read from Java user preferences and clamped or normalized before
     * callers consume them during bootstrap.</p>
     */
    public void load() {
        windowWidth = clamp(preferences.getInt(WIDTH_KEY, defaultWindowWidth), minWindowWidth, maxWindowWidth);
        windowHeight = clamp(preferences.getInt(HEIGHT_KEY, defaultWindowHeight), minWindowHeight, maxWindowHeight);
        hudAlpha = clamp(preferences.getDouble(HUD_ALPHA_KEY, 1.0), 0.0, 1.0);
        sayWindowAlpha = clamp(preferences.getDouble(SAY_WINDOW_ALPHA_KEY, 1.0), 0.0, 1.0);
        showPortrait = preferences.getBoolean(SHOW_PORTRAIT_KEY, true);
        cheatsVisible = preferences.getBoolean(CHEATS_VISIBLE_KEY, true);
        logStatChanges = preferences.getBoolean(LOG_STAT_CHANGES_KEY, false);
        footerShortcutDisplay = validatedFooterShortcutDisplay(footerShortcutDisplayPreferenceValue());
        footerIconDisplay = validatedFooterIconDisplay(
                preferences.get(FOOTER_ICON_DISPLAY_KEY, FooterIconDisplay.ICONS_WITH_TEXT.preferenceValue()));
        fontFamily = preferences.get(FONT_FAMILY_KEY, "System");
        fontScale = clamp(preferences.getDouble(FONT_SCALE_KEY, 1.0), fontScaleMin, fontScaleMax);
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
        // Default ON: the first time a new player starts the game (no stored value yet),
        // end-of-day auto-save is enabled.  Once the player toggles the Save-screen
        // checkbox the stored value wins.
        autoSaveDaily = preferences.getBoolean(AUTO_SAVE_DAILY_KEY, true);
        saveScreenViewMode = SaveScreenViewMode.fromPreferenceValue(
                preferences.get(SAVE_SCREEN_VIEW_MODE_KEY, SaveScreenViewMode.GRID.preferenceValue()));
        saveScreenPageCount = preferences.getInt(SAVE_SCREEN_PAGE_COUNT_KEY, 1);
        saveScreenSelectedPage = preferences.getInt(SAVE_SCREEN_SELECTED_PAGE_KEY, 1);
        language = validatedLanguage(preferences.get(LANGUAGE_KEY, Language.ENGLISH.preferenceValue()));
        textSpeed = validatedTextSpeed(preferences.get(TEXT_SPEED_KEY, TextSpeed.NORMAL.preferenceValue()));
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

    /** Returns whether the engine should auto-save once per in-game day. */
    public boolean autoSaveDaily() {
        return autoSaveDaily;
    }

    /** Persists the auto-save-daily flag and updates the loaded model. */
    public void saveAutoSaveDaily(boolean autoSaveDaily) {
        this.autoSaveDaily = autoSaveDaily;
        preferences.putBoolean(AUTO_SAVE_DAILY_KEY, autoSaveDaily);
        flushQuietly();
    }

    /** Returns the user's preferred presentation for the Save screen (grid or list). */
    public SaveScreenViewMode saveScreenViewMode() {
        return saveScreenViewMode;
    }

    /** Persists the Save-screen presentation mode and updates the loaded model. */
    public void saveSaveScreenViewMode(SaveScreenViewMode mode) {
        this.saveScreenViewMode = mode == null ? SaveScreenViewMode.GRID : mode;
        preferences.put(SAVE_SCREEN_VIEW_MODE_KEY, this.saveScreenViewMode.preferenceValue());
        flushQuietly();
    }

    /** Returns how many Save-screen pages the player has spawned (global, persisted).  Always
     *  at least 1.  Each page exposes a fixed block of slots, so this is the count the Save
     *  screen restores on restart so previously-added pages (and the saves on them) stay
     *  reachable. */
    public int saveScreenPageCount() {
        return Math.max(1, saveScreenPageCount);
    }

    /** Persists the Save-screen page count and updates the loaded model.  Clamped to a floor
     *  of 1; the Save screen enforces its own upper cap before calling this. */
    public void saveSaveScreenPageCount(int pageCount) {
        this.saveScreenPageCount = Math.max(1, pageCount);
        preferences.putInt(SAVE_SCREEN_PAGE_COUNT_KEY, this.saveScreenPageCount);
        flushQuietly();
    }

    /** Returns the Save-screen page that was last selected (global, persisted), so the screen
     *  re-opens on the same page between sessions.  Always at least 1; the Save screen clamps
     *  it against the live page count when applying. */
    public int saveScreenSelectedPage() {
        return Math.max(1, saveScreenSelectedPage);
    }

    /** Persists the last-selected Save-screen page and updates the loaded model.  Clamped to a
     *  floor of 1. */
    public void saveSaveScreenSelectedPage(int selectedPage) {
        this.saveScreenSelectedPage = Math.max(1, selectedPage);
        preferences.putInt(SAVE_SCREEN_SELECTED_PAGE_KEY, this.saveScreenSelectedPage);
        flushQuietly();
    }

    /** Forces the backing store to commit pending puts now rather than waiting for the
     *  JVM-exit shutdown hook.  Used by the save-screen checkboxes (Auto-save daily,
     *  Show as list) where the player expects their toggle to survive a restart even if
     *  the JVM is killed before the deferred preferences flush runs — common in
     *  fullscreen apps on Windows where closing the window doesn't always cleanly
     *  shut down the JVM.  Swallows {@link BackingStoreException} because a flush
     *  failure shouldn't blow up the click handler — at worst the value persists at
     *  the next clean exit instead of immediately. */
    private void flushQuietly() {
        try {
            preferences.flush();
        } catch (java.util.prefs.BackingStoreException ex) {
            System.err.println("[PreferencesService] Failed to flush preferences: " + ex);
        }
    }

    /** Save-screen presentation mode — grid (default, 5×4 tiled with thumbnails) or list (rows). */
    public enum SaveScreenViewMode {
        GRID("grid"),
        LIST("list");

        private final String preferenceValue;

        SaveScreenViewMode(String preferenceValue) {
            this.preferenceValue = preferenceValue;
        }

        public String preferenceValue() {
            return preferenceValue;
        }

        public static SaveScreenViewMode fromPreferenceValue(String value) {
            if (value == null) {
                return GRID;
            }
            for (SaveScreenViewMode mode : values()) {
                if (mode.preferenceValue.equals(value)) {
                    return mode;
                }
            }
            return GRID;
        }
    }

    /** Returns the preferred UI language. */
    public Language language() {
        return language;
    }

    /**
     * Returns the configured text speed, which drives consumers like the dialog block's
     * scroll-to-bottom animation duration. Defaults to {@link TextSpeed#NORMAL}.
     */
    public TextSpeed textSpeed() {
        return textSpeed;
    }

    /** Config-driven per-speed reveal/auto-advance durations (ms): {@code [slow, normal, fast]},
     *  or null to use the {@link TextSpeed} enum defaults. */
    private static volatile int[] textSpeedDurationOverride;

    /** Sets configurable text-speed durations (ms); any null arg keeps that speed's enum default.
     *  Pass all null to clear the override. */
    public static void setTextSpeedDurations(Integer slow, Integer normal, Integer fast) {
        if (slow == null && normal == null && fast == null) {
            textSpeedDurationOverride = null;
            return;
        }
        int[] durations = {
                TextSpeed.SLOW.durationMillis(),
                TextSpeed.NORMAL.durationMillis(),
                TextSpeed.FAST.durationMillis()};
        if (slow != null && slow > 0) {
            durations[0] = slow;
        }
        if (normal != null && normal > 0) {
            durations[1] = normal;
        }
        if (fast != null && fast > 0) {
            durations[2] = fast;
        }
        textSpeedDurationOverride = durations;
    }

    /** The reveal/auto-advance duration (ms) for the current text speed — the configured override
     *  when set, otherwise the {@link TextSpeed} enum default.  Consumers should prefer this over
     *  {@code textSpeed().durationMillis()} so the config durations take effect. */
    public int textSpeedMillis() {
        int[] override = textSpeedDurationOverride;
        if (override == null) {
            return textSpeed.durationMillis();
        }
        return switch (textSpeed) {
            case SLOW -> override[0];
            case NORMAL -> override[1];
            case FAST -> override[2];
        };
    }

    /** Persists a clamped window size separately from save-game state. */
    public void saveWindowSize(double width, double height) {
        preferences.putInt(WIDTH_KEY, clamp((int) Math.round(width), minWindowWidth, maxWindowWidth));
        preferences.putInt(HEIGHT_KEY, clamp((int) Math.round(height), minWindowHeight, maxWindowHeight));
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
        this.fontScale = clamp(fontScale, fontScaleMin, fontScaleMax);
        preferences.put(FONT_FAMILY_KEY, this.fontFamily);
        preferences.putDouble(FONT_SCALE_KEY, this.fontScale);
    }

    /** Persists just the global text-size scale, preserving the current font family.  Backs the
     *  Preferences "Text size" (Smaller / Normal / Bigger) control. */
    public void saveFontScale(double fontScale) {
        saveFontPreferences(this.fontFamily, fontScale);
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

    /** Persists the selected text speed, falling back to {@link TextSpeed#NORMAL} for null. */
    public void saveTextSpeed(TextSpeed textSpeed) {
        this.textSpeed = textSpeed == null ? TextSpeed.NORMAL : textSpeed;
        preferences.put(TEXT_SPEED_KEY, this.textSpeed.preferenceValue());
    }

    /** Persists a validated text-speed identifier, falling back to {@link TextSpeed#NORMAL} for unknown values. */
    public void saveTextSpeed(String textSpeed) {
        saveTextSpeed(validatedTextSpeed(textSpeed));
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

    private TextSpeed validatedTextSpeed(String value) {
        for (TextSpeed candidate : TextSpeed.values()) {
            if (candidate.preferenceValue().equals(value)) {
                return candidate;
            }
        }
        return TextSpeed.NORMAL;
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

    /**
     * Text-speed preference. The {@link #durationMillis()} value is fed to the dialog block's
     * scroll-to-bottom animation: slower speeds give the player more time to read a new line
     * before the panel pins to the bottom; faster speeds snap the view down quickly.
     */
    public enum TextSpeed {
        SLOW("slow", 800),
        NORMAL("normal", 400),
        FAST("fast", 200);

        private final String preferenceValue;
        private final int durationMillis;

        TextSpeed(String preferenceValue, int durationMillis) {
            this.preferenceValue = preferenceValue;
            this.durationMillis = durationMillis;
        }

        public String preferenceValue() {
            return preferenceValue;
        }

        /** Scroll-animation duration in milliseconds that consumers should use for this speed. */
        public int durationMillis() {
            return durationMillis;
        }

        public String label() {
            return SystemCodeTables.defaultCodeTitle(SystemCodeTables.TEXT_SPEED_TABLE_ID, preferenceValue);
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
