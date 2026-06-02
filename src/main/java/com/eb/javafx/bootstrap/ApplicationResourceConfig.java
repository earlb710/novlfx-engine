package com.eb.javafx.bootstrap;

import com.eb.javafx.resources.ResourceCategory;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.PathUtils;
import com.eb.javafx.util.SimpleJson;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * JSON-backed application resource configuration.
 *
 * <p>The active model is a per-category {@code resourceRoots} map plus a generic {@code resources} map for named
 * application overrides and a small set of typed startup default values for app/preferences/save-load screen
 * backgrounds. Earlier flat fields ({@code categoryCodeTablesPath}, {@code imageAssetRoot},
 * {@code jsonResourceRoot}) have been replaced by the registry-driven {@code resourceRoots} entries under
 * {@link ResourceCategory#SUPPORT}, {@link ResourceCategory#IMAGES}, and {@link ResourceCategory#UI}.</p>
 */
public final class ApplicationResourceConfig {
    private static final boolean DEFAULT_DEBUG = true;
    private static final String DEFAULT_BACKGROUND_VALUE = "";

    private final boolean debug;
    private final String defaultAppBackgroundColor;
    private final String defaultAppBackgroundImage;
    private final String defaultAppBackgroundImageTransparency;
    private final String defaultPreferencesScreenBackgroundColor;
    private final String defaultPreferencesScreenBackgroundImage;
    private final String defaultPreferencesScreenBackgroundImageTransparency;
    private final String defaultSaveLoadScreenBackgroundColor;
    private final String defaultSaveLoadScreenBackgroundImage;
    private final String defaultSaveLoadScreenBackgroundImageTransparency;
    private final Map<String, String> resources;
    private final Map<ResourceCategory, List<String>> resourceRoots;

    private ApplicationResourceConfig(
            boolean debug,
            String defaultAppBackgroundColor,
            String defaultAppBackgroundImage,
            String defaultAppBackgroundImageTransparency,
            String defaultPreferencesScreenBackgroundColor,
            String defaultPreferencesScreenBackgroundImage,
            String defaultPreferencesScreenBackgroundImageTransparency,
            String defaultSaveLoadScreenBackgroundColor,
            String defaultSaveLoadScreenBackgroundImage,
            String defaultSaveLoadScreenBackgroundImageTransparency,
            Map<String, String> resources,
            Map<ResourceCategory, List<String>> resourceRoots) {
        this.debug = debug;
        this.defaultAppBackgroundColor = Validation.requireNonNull(
                defaultAppBackgroundColor, "Default app background color is required.");
        this.defaultAppBackgroundImage = Validation.requireNonNull(
                defaultAppBackgroundImage, "Default app background image is required.");
        this.defaultAppBackgroundImageTransparency = Validation.requireNonNull(
                defaultAppBackgroundImageTransparency, "Default app background image transparency is required.");
        this.defaultPreferencesScreenBackgroundColor = Validation.requireNonNull(
                defaultPreferencesScreenBackgroundColor, "Default preferences screen background color is required.");
        this.defaultPreferencesScreenBackgroundImage = Validation.requireNonNull(
                defaultPreferencesScreenBackgroundImage, "Default preferences screen background image is required.");
        this.defaultPreferencesScreenBackgroundImageTransparency = Validation.requireNonNull(
                defaultPreferencesScreenBackgroundImageTransparency,
                "Default preferences screen background image transparency is required.");
        this.defaultSaveLoadScreenBackgroundColor = Validation.requireNonNull(
                defaultSaveLoadScreenBackgroundColor, "Default save/load screen background color is required.");
        this.defaultSaveLoadScreenBackgroundImage = Validation.requireNonNull(
                defaultSaveLoadScreenBackgroundImage, "Default save/load screen background image is required.");
        this.defaultSaveLoadScreenBackgroundImageTransparency = Validation.requireNonNull(
                defaultSaveLoadScreenBackgroundImageTransparency,
                "Default save/load screen background image transparency is required.");
        LinkedHashMap<String, String> validatedResources = new LinkedHashMap<>();
        Validation.requireNonNull(resources, "Application resource config resources map is required.")
                .forEach((key, value) -> validatedResources.put(
                        Validation.requireNonBlank(key, "Application resource config resource ID is required."),
                        Validation.requireNonBlank(value, "Application resource config resource path is required.")));
        this.resources = Map.copyOf(validatedResources);
        this.resourceRoots = copyResourceRoots(Validation.requireNonNull(
                resourceRoots, "Application resource config resourceRoots map is required."));
    }

    public static ApplicationResourceConfig defaults() {
        return new ApplicationResourceConfig(
                DEFAULT_DEBUG,
                DEFAULT_BACKGROUND_VALUE, DEFAULT_BACKGROUND_VALUE, DEFAULT_BACKGROUND_VALUE,
                DEFAULT_BACKGROUND_VALUE, DEFAULT_BACKGROUND_VALUE, DEFAULT_BACKGROUND_VALUE,
                DEFAULT_BACKGROUND_VALUE, DEFAULT_BACKGROUND_VALUE, DEFAULT_BACKGROUND_VALUE,
                Map.of(),
                Map.of());
    }

    /** Returns a config carrying only the supplied generic resource overrides. */
    public static ApplicationResourceConfig of(Map<String, String> resources) {
        return defaults().withResources(resources);
    }

    public static ApplicationResourceConfig load(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Application resource config JSON path is required.");
        try {
            return fromJson(Files.readString(jsonPath, StandardCharsets.UTF_8), jsonPath.toString());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read application resource config JSON: " + jsonPath, exception);
        }
    }

    public static ApplicationResourceConfig fromJson(String json, String sourceName) {
        Map<String, Object> root = requireObject(SimpleJson.parse(json, sourceName), "root");
        ApplicationResourceConfig defaults = defaults();
        // First-class modding fields (fonts / assetOverrideRoot / windowTitle / appIcon /
        // uiTheme / themePalette) are folded into the generic `resources` map at parse time, so
        // downstream code keeps reading them by their reserved ids and the equivalent
        // `resources` entries still work for back-compat.  Explicit top-level fields win.
        Map<String, String> resources = promoteFirstClassFields(root,
                optionalObject(root, "resources", "root.resources")
                        .map(ApplicationResourceConfig::toStringMap)
                        .orElse(Map.of()));
        return new ApplicationResourceConfig(
                optionalBoolean(root, "debug", "root.debug").orElse(defaults.debug()),
                optionalStringAllowingBlank(root, "defaultAppBackgroundColor", "root.defaultAppBackgroundColor")
                        .orElse(defaults.defaultAppBackgroundColor()),
                optionalStringAllowingBlank(root, "defaultAppBackgroundImage", "root.defaultAppBackgroundImage")
                        .orElse(defaults.defaultAppBackgroundImage()),
                optionalStringAllowingBlank(root, "defaultAppBackgroundImageTransparency",
                        "root.defaultAppBackgroundImageTransparency")
                        .orElse(defaults.defaultAppBackgroundImageTransparency()),
                optionalStringAllowingBlank(root, "defaultPreferencesScreenBackgroundColor",
                        "root.defaultPreferencesScreenBackgroundColor")
                        .orElse(defaults.defaultPreferencesScreenBackgroundColor()),
                optionalStringAllowingBlank(root, "defaultPreferencesScreenBackgroundImage",
                        "root.defaultPreferencesScreenBackgroundImage")
                        .orElse(defaults.defaultPreferencesScreenBackgroundImage()),
                optionalStringAllowingBlank(root, "defaultPreferencesScreenBackgroundImageTransparency",
                        "root.defaultPreferencesScreenBackgroundImageTransparency")
                        .orElse(defaults.defaultPreferencesScreenBackgroundImageTransparency()),
                optionalStringAllowingBlank(root, "defaultSaveLoadScreenBackgroundColor",
                        "root.defaultSaveLoadScreenBackgroundColor")
                        .orElse(defaults.defaultSaveLoadScreenBackgroundColor()),
                optionalStringAllowingBlank(root, "defaultSaveLoadScreenBackgroundImage",
                        "root.defaultSaveLoadScreenBackgroundImage")
                        .orElse(defaults.defaultSaveLoadScreenBackgroundImage()),
                optionalStringAllowingBlank(root, "defaultSaveLoadScreenBackgroundImageTransparency",
                        "root.defaultSaveLoadScreenBackgroundImageTransparency")
                        .orElse(defaults.defaultSaveLoadScreenBackgroundImageTransparency()),
                resources,
                optionalObject(root, "resourceRoots", "root.resourceRoots")
                        .map(ApplicationResourceConfig::toResourceRootsMap)
                        .orElse(Map.of()));
    }

    /** Reserved single-value first-class field ids that map 1:1 onto a {@code resources} entry. */
    private static final List<String> FIRST_CLASS_STRING_FIELDS = List.of(
            "assetOverrideRoot", "windowTitle", "appIcon", "uiTheme", "themePalette",
            "mapBuildingColors");

    /**
     * Folds the explicit top-level modding fields into a copy of {@code resources}.  String
     * fields become same-named entries; the {@code fonts} array becomes {@code font.cfgN}
     * entries (so {@link com.eb.javafx.util.FontResources}-driven registration picks them up).
     * Explicit top-level values override any same-id {@code resources} entry.  Validates types.
     */
    private static Map<String, String> promoteFirstClassFields(
            Map<String, Object> root, Map<String, String> resources) {
        LinkedHashMap<String, String> merged = new LinkedHashMap<>(resources);
        for (String field : FIRST_CLASS_STRING_FIELDS) {
            optionalStringAllowingBlank(root, field, "root." + field)
                    .filter(value -> !value.isBlank())
                    .ifPresent(value -> merged.put(field, value));
        }
        if (root.containsKey("fonts")) {
            Object value = root.get("fonts");
            if (!(value instanceof List<?> list)) {
                throw new IllegalArgumentException("Expected JSON array for root.fonts.");
            }
            int index = 0;
            for (Object element : list) {
                if (!(element instanceof String fontPath) || fontPath.isBlank()) {
                    throw new IllegalArgumentException(
                            "Expected non-blank JSON string for root.fonts[" + index + "].");
                }
                merged.put("font.cfg" + index, fontPath);
                index++;
            }
        }
        promoteScreenBackgrounds(root, merged);
        promoteFooterStyle(root, merged);
        promoteFooterOptions(root, merged);
        promoteTextSpeed(root, merged);
        promoteScalar(root, "tooltipDelayMs", merged);
        promoteAudioChannels(root, merged);
        promoteAutoAdvance(root, merged);
        promoteHud(root, merged);
        promoteUiDialog(root, merged);
        promoteSave(root, merged);
        promoteWindow(root, merged);
        promoteDisplay(root, merged);
        promoteText(root, merged);
        return merged;
    }

    public static final String UI_PREFIX = "ui.";
    public static final String UI_DIALOG_PREFIX = "ui.dialog.";
    public static final String UI_SPACING_PREFIX = "ui.spacing.";

    /** Folds the top-level {@code ui} object: the nested {@code dialog} object
     *  ({@code minWidth / maxWidth / previousEntryOpacity}) into {@code ui.dialog.<field>}, the
     *  nested {@code spacing} object ({@code body / outer / panel / footer}) into
     *  {@code ui.spacing.<field>}, and the scalar accessibility fields
     *  {@code fontScaleMin / fontScaleMax} into {@code ui.<field>}. */
    private static void promoteUiDialog(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("ui")) {
            return;
        }
        Map<String, Object> ui = requireObject(root.get("ui"), "root.ui");
        ui.forEach((key, value) -> {
            switch (key) {
                case "dialog" -> {
                    Map<String, Object> fields = requireObject(value, "root.ui.dialog");
                    fields.forEach((field, raw) -> {
                        if (!Set.of("minWidth", "maxWidth", "previousEntryOpacity").contains(field)) {
                            throw new IllegalArgumentException("Unknown ui.dialog field '" + field
                                    + "' (use minWidth / maxWidth / previousEntryOpacity).");
                        }
                        merged.put(UI_DIALOG_PREFIX + field,
                                requireScalarString(raw, "root.ui.dialog." + field));
                    });
                }
                case "spacing" -> {
                    Map<String, Object> fields = requireObject(value, "root.ui.spacing");
                    fields.forEach((field, raw) -> {
                        if (!Set.of("body", "outer", "panel", "footer").contains(field)) {
                            throw new IllegalArgumentException("Unknown ui.spacing field '" + field
                                    + "' (use body / outer / panel / footer).");
                        }
                        merged.put(UI_SPACING_PREFIX + field,
                                requireScalarString(raw, "root.ui.spacing." + field));
                    });
                }
                case "fontScaleMin", "fontScaleMax" ->
                        merged.put(UI_PREFIX + key, requireScalarString(value, "root.ui." + key));
                default -> throw new IllegalArgumentException("Unknown ui field '" + key
                        + "' (use dialog / spacing / fontScaleMin / fontScaleMax).");
            }
        });
    }

    /** Top-level {@code ui} scalar field (e.g. {@code fontScaleMin / fontScaleMax}), if set. */
    public Optional<String> uiField(String field) {
        return Optional.ofNullable(resources.get(UI_PREFIX + field));
    }

    /** {@code ui.spacing} field ({@code body / outer / panel / footer}), if set. */
    public Optional<String> uiSpacingField(String field) {
        return Optional.ofNullable(resources.get(UI_SPACING_PREFIX + field));
    }

    public static final String SAVE_PREFIX = "save.";

    /** Folds {@code save: { maxHistoryEntries, gridThumbnailWidth, gridThumbnailHeight,
     *  listThumbnailWidth, listThumbnailHeight, thumbnailWidth, thumbnailHeight,
     *  thumbnailJpegQuality }} into {@code save.<field>} resources entries (history cap + save-tile
     *  thumbnail dimensions + persisted-thumbnail encoding). */
    private static void promoteSave(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("save")) {
            return;
        }
        Map<String, Object> fields = requireObject(root.get("save"), "root.save");
        fields.forEach((field, raw) -> {
            if (!Set.of("maxHistoryEntries", "gridThumbnailWidth", "gridThumbnailHeight",
                    "listThumbnailWidth", "listThumbnailHeight",
                    "thumbnailWidth", "thumbnailHeight", "thumbnailJpegQuality").contains(field)) {
                throw new IllegalArgumentException("Unknown save field '" + field + "' (use "
                        + "maxHistoryEntries / gridThumbnailWidth / gridThumbnailHeight"
                        + " / listThumbnailWidth / listThumbnailHeight"
                        + " / thumbnailWidth / thumbnailHeight / thumbnailJpegQuality).");
            }
            merged.put(SAVE_PREFIX + field, requireScalarString(raw, "root.save." + field));
        });
    }

    /** Save-system field (e.g. {@code maxHistoryEntries}), if set. */
    public Optional<String> saveField(String field) {
        return Optional.ofNullable(resources.get(SAVE_PREFIX + field));
    }

    public static final String WINDOW_PREFIX = "window.";

    /** Folds {@code window: { defaultWidth, defaultHeight, minWidth, maxWidth, minHeight, maxHeight }}
     *  into {@code window.<field>} resources entries (startup window size + clamp bounds). */
    private static void promoteWindow(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("window")) {
            return;
        }
        Map<String, Object> fields = requireObject(root.get("window"), "root.window");
        fields.forEach((field, raw) -> {
            if (!Set.of("defaultWidth", "defaultHeight", "minWidth", "maxWidth",
                    "minHeight", "maxHeight").contains(field)) {
                throw new IllegalArgumentException("Unknown window field '" + field + "' (use "
                        + "defaultWidth / defaultHeight / minWidth / maxWidth / minHeight / maxHeight).");
            }
            merged.put(WINDOW_PREFIX + field, requireScalarString(raw, "root.window." + field));
        });
    }

    /** Window-sizing field (e.g. {@code defaultWidth / minWidth}), if set. */
    public Optional<String> windowField(String field) {
        return Optional.ofNullable(resources.get(WINDOW_PREFIX + field));
    }

    public static final String DISPLAY_PREFIX = "display.";

    /** Folds {@code display: { svgBackgroundMinRaster: { width, height } }} into
     *  {@code display.svgBackgroundMinRaster.<field>} resources entries. */
    private static void promoteDisplay(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("display")) {
            return;
        }
        Map<String, Object> display = requireObject(root.get("display"), "root.display");
        display.forEach((key, value) -> {
            if (!"svgBackgroundMinRaster".equals(key)) {
                throw new IllegalArgumentException("Unknown display field '" + key
                        + "' (use svgBackgroundMinRaster).");
            }
            Map<String, Object> fields = requireObject(value, "root.display.svgBackgroundMinRaster");
            fields.forEach((field, raw) -> {
                if (!Set.of("width", "height").contains(field)) {
                    throw new IllegalArgumentException("Unknown display.svgBackgroundMinRaster field '"
                            + field + "' (use width / height).");
                }
                merged.put(DISPLAY_PREFIX + "svgBackgroundMinRaster." + field,
                        requireScalarString(raw, "root.display.svgBackgroundMinRaster." + field));
            });
        });
    }

    /** Display field (e.g. {@code svgBackgroundMinRaster.width}), if set. */
    public Optional<String> displayField(String field) {
        return Optional.ofNullable(resources.get(DISPLAY_PREFIX + field));
    }

    public static final String TEXT_KINETIC_PREFIX = "text.kineticEffects.";

    /** Folds {@code text: { kineticEffects: { pulse, float, shake } }} into
     *  {@code text.kineticEffects.<field>} resources entries (kinetic text-effect durations, ms). */
    private static void promoteText(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("text")) {
            return;
        }
        Map<String, Object> text = requireObject(root.get("text"), "root.text");
        text.forEach((key, value) -> {
            if (!"kineticEffects".equals(key)) {
                throw new IllegalArgumentException("Unknown text field '" + key
                        + "' (use kineticEffects).");
            }
            Map<String, Object> fields = requireObject(value, "root.text.kineticEffects");
            fields.forEach((field, raw) -> {
                if (!Set.of("pulse", "float", "shake").contains(field)) {
                    throw new IllegalArgumentException("Unknown text.kineticEffects field '" + field
                            + "' (use pulse / float / shake).");
                }
                merged.put(TEXT_KINETIC_PREFIX + field,
                        requireScalarString(raw, "root.text.kineticEffects." + field));
            });
        });
    }

    /** Kinetic text-effect duration field ({@code pulse / float / shake}, ms), if set. */
    public Optional<String> textKineticField(String field) {
        return Optional.ofNullable(resources.get(TEXT_KINETIC_PREFIX + field));
    }

    /** Dialog (confirm/info/error popup) card-width field, if set. */
    public Optional<String> uiDialogField(String field) {
        return Optional.ofNullable(resources.get(UI_DIALOG_PREFIX + field));
    }

    public static final String HUD_PREFIX = "hud.";

    /** Folds {@code hud: { dialogIdleAlpha, dialogActiveAlpha, locationRestAlpha, locationHoverAlpha }}
     *  into {@code hud.<field>} resources entries (gameplay HUD backdrop opacity). */
    private static void promoteHud(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("hud")) {
            return;
        }
        Map<String, Object> fields = requireObject(root.get("hud"), "root.hud");
        fields.forEach((field, raw) -> {
            if (!Set.of("dialogIdleAlpha", "dialogActiveAlpha", "locationRestAlpha",
                    "locationHoverAlpha", "statusLogAlpha", "panelAlpha").contains(field)) {
                throw new IllegalArgumentException("Unknown hud field '" + field + "' (use "
                        + "dialogIdleAlpha / dialogActiveAlpha / locationRestAlpha / locationHoverAlpha"
                        + " / statusLogAlpha / panelAlpha).");
            }
            merged.put(HUD_PREFIX + field, requireScalarString(raw, "root.hud." + field));
        });
    }

    /** HUD backdrop-alpha field, if set. */
    public Optional<String> hudField(String field) {
        return Optional.ofNullable(resources.get(HUD_PREFIX + field));
    }

    public static final String AUDIO_CHANNEL_PREFIX = "audioChannel.";
    public static final String AUTO_ADVANCE_PREFIX = "autoAdvance.";

    /** Folds {@code audioChannels: { "<id>": { priority, volume, ducking, duckPercent } }} into
     *  {@code audioChannel.<id>.<field>} resources entries. */
    private static void promoteAudioChannels(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("audioChannels")) {
            return;
        }
        Map<String, Object> channels = requireObject(root.get("audioChannels"), "root.audioChannels");
        channels.forEach((channelId, value) -> {
            Map<String, Object> fields = requireObject(value, "root.audioChannels." + channelId);
            fields.forEach((field, raw) -> {
                if (!Set.of("priority", "volume", "ducking", "duckPercent").contains(field)) {
                    throw new IllegalArgumentException("Unknown audio channel field '" + field
                            + "' in root.audioChannels." + channelId
                            + " (use priority / volume / ducking / duckPercent).");
                }
                merged.put(AUDIO_CHANNEL_PREFIX + channelId + "." + field,
                        requireScalarString(raw, "root.audioChannels." + channelId + "." + field));
            });
        });
    }

    /** Folds {@code autoAdvance: { scrollFraction, minScrollMs, readPauseMultiplier }} into
     *  {@code autoAdvance.<field>} resources entries. */
    private static void promoteAutoAdvance(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("autoAdvance")) {
            return;
        }
        Map<String, Object> fields = requireObject(root.get("autoAdvance"), "root.autoAdvance");
        fields.forEach((field, raw) -> {
            if (!Set.of("scrollFraction", "minScrollMs", "readPauseMultiplier").contains(field)) {
                throw new IllegalArgumentException("Unknown autoAdvance field '" + field
                        + "' (use scrollFraction / minScrollMs / readPauseMultiplier).");
            }
            merged.put(AUTO_ADVANCE_PREFIX + field, requireScalarString(raw, "root.autoAdvance." + field));
        });
    }

    /** Audio-channel override field ({@code priority}/{@code volume}/{@code ducking}/
     *  {@code duckPercent}) for {@code channelId}, if set. */
    public Optional<String> audioChannelField(String channelId, String field) {
        return Optional.ofNullable(resources.get(AUDIO_CHANNEL_PREFIX + channelId + "." + field));
    }

    /** Auto-advance tuning field ({@code scrollFraction}/{@code minScrollMs}/
     *  {@code readPauseMultiplier}), if set. */
    public Optional<String> autoAdvanceField(String field) {
        return Optional.ofNullable(resources.get(AUTO_ADVANCE_PREFIX + field));
    }

    /** Reserved {@code resources} id prefixes for footer-option (keybinding/icon) overrides and
     *  per-speed text-speed durations. */
    public static final String FOOTER_OPTION_PREFIX = "footerOption.";
    public static final String TEXT_SPEED_PREFIX = "textSpeed.";

    /** Folds {@code footerOptions: { "<id>": { shortcut, icon } }} into
     *  {@code footerOption.<id>.<field>} resources entries (footer keybinding / glyph mods). */
    private static void promoteFooterOptions(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("footerOptions")) {
            return;
        }
        Map<String, Object> options = requireObject(root.get("footerOptions"), "root.footerOptions");
        options.forEach((optionId, value) -> {
            Map<String, Object> fields = requireObject(value, "root.footerOptions." + optionId);
            fields.forEach((rawField, rawValue) -> {
                String field = switch (rawField) {
                    case "shortcut", "key" -> "shortcut";
                    case "icon", "glyph" -> "icon";
                    default -> null;
                };
                if (field == null) {
                    throw new IllegalArgumentException("Unknown footer option field '" + rawField
                            + "' in root.footerOptions." + optionId + " (use shortcut / icon).");
                }
                if (!(rawValue instanceof String stringValue)) {
                    throw new IllegalArgumentException("Expected JSON string for root.footerOptions."
                            + optionId + "." + rawField + ".");
                }
                if (!stringValue.isBlank()) {
                    merged.put(FOOTER_OPTION_PREFIX + optionId + "." + field, stringValue);
                }
            });
        });
    }

    /** Folds {@code textSpeed: { slow, normal, fast }} (millisecond reveal/auto-advance times)
     *  into {@code textSpeed.<speed>} resources entries. */
    private static void promoteTextSpeed(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("textSpeed")) {
            return;
        }
        Map<String, Object> speeds = requireObject(root.get("textSpeed"), "root.textSpeed");
        speeds.forEach((speed, value) -> {
            if (!(speed.equals("slow") || speed.equals("normal") || speed.equals("fast"))) {
                throw new IllegalArgumentException("Unknown text speed '" + speed
                        + "' in root.textSpeed (use slow / normal / fast).");
            }
            merged.put(TEXT_SPEED_PREFIX + speed, requireScalarString(value, "root.textSpeed." + speed));
        });
    }

    /** Folds a top-level scalar (string or number) field into the resources map. */
    private static void promoteScalar(Map<String, Object> root, String key, Map<String, String> merged) {
        if (root.containsKey(key)) {
            merged.put(key, requireScalarString(root.get(key), "root." + key));
        }
    }

    private static String requireScalarString(Object value, String description) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        if (value instanceof Number number) {
            // Drop a trailing .0 for whole-number millis.
            String text = number.toString();
            return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
        }
        throw new IllegalArgumentException("Expected a string or number for " + description + ".");
    }

    /** Footer-option override field ({@code shortcut} / {@code icon}) for {@code optionId}, if set. */
    public Optional<String> footerOptionOverride(String optionId, String field) {
        return Optional.ofNullable(resources.get(FOOTER_OPTION_PREFIX + optionId + "." + field));
    }

    /** All footer option ids that carry an override (so a host can iterate them). */
    public java.util.Set<String> footerOptionOverrideIds() {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        for (String key : resources.keySet()) {
            if (key.startsWith(FOOTER_OPTION_PREFIX)) {
                int dot = key.indexOf('.', FOOTER_OPTION_PREFIX.length());
                if (dot > 0) {
                    ids.add(key.substring(FOOTER_OPTION_PREFIX.length(), dot));
                }
            }
        }
        return ids;
    }

    /** Configured reveal/auto-advance milliseconds for a text speed ({@code slow}/{@code normal}/
     *  {@code fast}), if set. */
    public Optional<String> textSpeedMillis(String speed) {
        return Optional.ofNullable(resources.get(TEXT_SPEED_PREFIX + speed));
    }

    /** Configured global tooltip show-delay in milliseconds, if set. */
    public Optional<String> tooltipDelayMillis() {
        return Optional.ofNullable(resources.get("tooltipDelayMs"));
    }

    /** Reserved {@code resources} id prefix for a footer style field, e.g. {@code footer.color}. */
    public static final String FOOTER_STYLE_PREFIX = "footer.";

    /**
     * Folds the optional top-level {@code footer} object — {@code { font, color, selectColor,
     * backgroundColor, transparency }} — into {@code resources} entries {@code footer.<field>}.
     */
    private static void promoteFooterStyle(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("footer")) {
            return;
        }
        Map<String, Object> footer = requireObject(root.get("footer"), "root.footer");
        footer.forEach((rawField, rawValue) -> {
            String field = normaliseFooterField(rawField);
            if (field == null) {
                throw new IllegalArgumentException("Unknown footer style field '" + rawField
                        + "' in root.footer (use font / color / selectColor / backgroundColor"
                        + " / transparency / restOpacity / hoverOpacity).");
            }
            if (isNumericFooterField(field)) {
                merged.put(FOOTER_STYLE_PREFIX + field,
                        requireScalarString(rawValue, "root.footer." + rawField));
                return;
            }
            if (!(rawValue instanceof String stringValue)) {
                throw new IllegalArgumentException("Expected JSON string for root.footer." + rawField + ".");
            }
            if (!stringValue.isBlank()) {
                merged.put(FOOTER_STYLE_PREFIX + field, stringValue);
            }
        });
    }

    private static String normaliseFooterField(String field) {
        return switch (field) {
            case "font", "fontFamily" -> "font";
            case "color", "textColor" -> "color";
            case "selectColor", "activeColor", "highlightColor" -> "selectColor";
            case "backgroundColor", "background" -> "backgroundColor";
            case "transparency", "opacity" -> "transparency";
            case "restOpacity" -> "restOpacity";
            case "hoverOpacity" -> "hoverOpacity";
            default -> null;
        };
    }

    /** Footer fields whose value is a number (opacity) rather than a CSS string. */
    private static boolean isNumericFooterField(String field) {
        return "restOpacity".equals(field) || "hoverOpacity".equals(field);
    }

    /** Configured footer style field ({@code font} / {@code color} / {@code selectColor} /
     *  {@code backgroundColor} / {@code transparency}), if set. */
    public Optional<String> footerStyle(String field) {
        return Optional.ofNullable(resources.get(FOOTER_STYLE_PREFIX + field));
    }

    /** Reserved {@code resources} id prefix for a per-screen background field, keyed by screen
     *  (route) id, e.g. {@code screenBackground.main-menu.color}. */
    public static final String SCREEN_BACKGROUND_PREFIX = "screenBackground.";

    /**
     * Folds the optional top-level {@code screenBackgrounds} object — a per-screen (route-id)
     * map of {@code { color, image, transparency }} — into {@code resources} entries
     * {@code screenBackground.<key>.<field>}.  Field names accept friendly or long forms.
     */
    private static void promoteScreenBackgrounds(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("screenBackgrounds")) {
            return;
        }
        Map<String, Object> screens = requireObject(root.get("screenBackgrounds"), "root.screenBackgrounds");
        screens.forEach((screenKey, value) -> {
            Map<String, Object> fields = requireObject(value, "root.screenBackgrounds." + screenKey);
            fields.forEach((rawField, rawValue) -> {
                String field = normaliseBackgroundField(rawField);
                if (field == null) {
                    throw new IllegalArgumentException(
                            "Unknown screen background field '" + rawField + "' in root.screenBackgrounds."
                                    + screenKey + " (use color / image / transparency).");
                }
                if (!(rawValue instanceof String stringValue)) {
                    throw new IllegalArgumentException("Expected JSON string for root.screenBackgrounds."
                            + screenKey + "." + rawField + ".");
                }
                if (!stringValue.isBlank()) {
                    merged.put(SCREEN_BACKGROUND_PREFIX + screenKey + "." + field, stringValue);
                }
            });
        });
    }

    private static String normaliseBackgroundField(String field) {
        return switch (field) {
            case "color", "backgroundColor" -> "color";
            case "image", "backgroundImage" -> "image";
            case "transparency", "imageTransparency", "backgroundImageTransparency" -> "transparency";
            default -> null;
        };
    }

    /** Per-screen background colour for {@code screenKey} (a route id), if configured. */
    public Optional<String> screenBackgroundColor(String screenKey) {
        return screenBackgroundField(screenKey, "color");
    }

    /** Per-screen background image for {@code screenKey} (a route id), if configured. */
    public Optional<String> screenBackgroundImage(String screenKey) {
        return screenBackgroundField(screenKey, "image");
    }

    /** Per-screen background image transparency for {@code screenKey} (a route id), if configured. */
    public Optional<String> screenBackgroundImageTransparency(String screenKey) {
        return screenBackgroundField(screenKey, "transparency");
    }

    private Optional<String> screenBackgroundField(String screenKey, String field) {
        if (screenKey == null || screenKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(resources.get(SCREEN_BACKGROUND_PREFIX + screenKey + "." + field));
    }

    public boolean debug() {
        return debug;
    }

    public String defaultAppBackgroundColor() {
        return defaultAppBackgroundColor;
    }

    public String defaultAppBackgroundImage() {
        return defaultAppBackgroundImage;
    }

    public String defaultAppBackgroundImageTransparency() {
        return defaultAppBackgroundImageTransparency;
    }

    public String defaultPreferencesScreenBackgroundColor() {
        return defaultPreferencesScreenBackgroundColor;
    }

    public String defaultPreferencesScreenBackgroundImage() {
        return defaultPreferencesScreenBackgroundImage;
    }

    public String defaultPreferencesScreenBackgroundImageTransparency() {
        return defaultPreferencesScreenBackgroundImageTransparency;
    }

    public String defaultSaveLoadScreenBackgroundColor() {
        return defaultSaveLoadScreenBackgroundColor;
    }

    public String defaultSaveLoadScreenBackgroundImage() {
        return defaultSaveLoadScreenBackgroundImage;
    }

    public String defaultSaveLoadScreenBackgroundImageTransparency() {
        return defaultSaveLoadScreenBackgroundImageTransparency;
    }

    public Map<String, String> resources() {
        return resources;
    }

    public Optional<String> resourcePath(String resourceId) {
        Validation.requireNonBlank(resourceId, "Application resource config resource ID is required.");
        return Optional.ofNullable(resources.get(resourceId));
    }

    public Optional<Path> resolveResource(Path baseDirectory, String resourceId) {
        return resourcePath(resourceId).map(path -> PathUtils.resolveChild(baseDirectory, path));
    }

    /**
     * Returns the configured per-category resource roots in declaration order. Application bootstrap layers these
     * over the library's bundled roots when building a {@code ResourceRegistry}.
     */
    public Map<ResourceCategory, List<String>> resourceRoots() {
        return resourceRoots;
    }

    public List<String> resourceRoots(ResourceCategory category) {
        Validation.requireNonNull(category, "Resource category is required.");
        return resourceRoots.getOrDefault(category, List.of());
    }

    public ApplicationResourceConfig withDebug(boolean debug) {
        return new ApplicationResourceConfig(
                debug,
                defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultAppBackgroundColor(String value) {
        return new ApplicationResourceConfig(
                debug, value, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultAppBackgroundImage(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, value, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultAppBackgroundImageTransparency(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, value,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultPreferencesScreenBackgroundColor(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                value, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultPreferencesScreenBackgroundImage(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, value,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultPreferencesScreenBackgroundImageTransparency(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage, value,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultSaveLoadScreenBackgroundColor(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                value, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultSaveLoadScreenBackgroundImage(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, value,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultSaveLoadScreenBackgroundImageTransparency(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage, value,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withResources(Map<String, String> resources) {
        return new ApplicationResourceConfig(
                debug,
                defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig putResource(String resourceId, String resourcePath) {
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(resources);
        updated.put(resourceId, resourcePath);
        return withResources(updated);
    }

    public ApplicationResourceConfig removeResource(String resourceId) {
        Validation.requireNonBlank(resourceId, "Application resource config resource ID is required.");
        if (!resources.containsKey(resourceId)) {
            throw new IllegalArgumentException("Unknown application resource: " + resourceId);
        }
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(resources);
        updated.remove(resourceId);
        return withResources(updated);
    }

    /** Returns a copy with the given per-category resource roots, replacing any previously configured roots. */
    public ApplicationResourceConfig withResourceRoots(Map<ResourceCategory, List<String>> resourceRoots) {
        return new ApplicationResourceConfig(
                debug,
                defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    /** Returns a copy with one additional root spec appended to the given category's existing list. */
    public ApplicationResourceConfig withAdditionalResourceRoot(ResourceCategory category, String rootSpec) {
        Validation.requireNonNull(category, "Resource category is required.");
        Validation.requireNonBlank(rootSpec, "Resource root spec is required.");
        EnumMap<ResourceCategory, List<String>> updated = new EnumMap<>(ResourceCategory.class);
        updated.putAll(resourceRoots);
        List<String> existing = updated.getOrDefault(category, List.of());
        List<String> combined = new ArrayList<>(existing.size() + 1);
        combined.addAll(existing);
        combined.add(rootSpec);
        updated.put(category, List.copyOf(combined));
        return withResourceRoots(updated);
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
                .append("  \"debug\": ").append(debug).append(",\n")
                .append("  \"defaultAppBackgroundColor\": ").append(JsonStrings.quote(defaultAppBackgroundColor)).append(",\n")
                .append("  \"defaultAppBackgroundImage\": ").append(JsonStrings.quote(defaultAppBackgroundImage)).append(",\n")
                .append("  \"defaultAppBackgroundImageTransparency\": ")
                .append(JsonStrings.quote(defaultAppBackgroundImageTransparency)).append(",\n")
                .append("  \"defaultPreferencesScreenBackgroundColor\": ")
                .append(JsonStrings.quote(defaultPreferencesScreenBackgroundColor)).append(",\n")
                .append("  \"defaultPreferencesScreenBackgroundImage\": ")
                .append(JsonStrings.quote(defaultPreferencesScreenBackgroundImage)).append(",\n")
                .append("  \"defaultPreferencesScreenBackgroundImageTransparency\": ")
                .append(JsonStrings.quote(defaultPreferencesScreenBackgroundImageTransparency)).append(",\n")
                .append("  \"defaultSaveLoadScreenBackgroundColor\": ")
                .append(JsonStrings.quote(defaultSaveLoadScreenBackgroundColor)).append(",\n")
                .append("  \"defaultSaveLoadScreenBackgroundImage\": ")
                .append(JsonStrings.quote(defaultSaveLoadScreenBackgroundImage)).append(",\n")
                .append("  \"defaultSaveLoadScreenBackgroundImageTransparency\": ")
                .append(JsonStrings.quote(defaultSaveLoadScreenBackgroundImageTransparency)).append(",\n")
                .append("  \"resources\": {\n");
        int index = 0;
        for (Map.Entry<String, String> entry : resources.entrySet()) {
            json.append("    ")
                    .append(JsonStrings.quote(entry.getKey()))
                    .append(": ")
                    .append(JsonStrings.quote(entry.getValue()));
            if (index + 1 < resources.size()) {
                json.append(',');
            }
            json.append('\n');
            index++;
        }
        json.append("  },\n");
        json.append("  \"resourceRoots\": {\n");
        int rootIndex = 0;
        int rootCount = resourceRoots.size();
        for (Map.Entry<ResourceCategory, List<String>> entry : resourceRoots.entrySet()) {
            json.append("    ")
                    .append(JsonStrings.quote(entry.getKey().configKey()))
                    .append(": [");
            List<String> values = entry.getValue();
            for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
                json.append(JsonStrings.quote(values.get(valueIndex)));
                if (valueIndex + 1 < values.size()) {
                    json.append(", ");
                }
            }
            json.append(']');
            if (rootIndex + 1 < rootCount) {
                json.append(',');
            }
            json.append('\n');
            rootIndex++;
        }
        json.append("  }\n");
        json.append("}\n");
        return json.toString();
    }

    public void save(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Application resource config JSON path is required.");
        try {
            Files.writeString(jsonPath, toJson(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to write application resource config JSON: " + jsonPath, exception);
        }
    }

    private static Map<String, Object> requireObject(Object value, String description) {
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> {
                if (!(key instanceof String stringKey)) {
                    throw new IllegalArgumentException("JSON object key must be a string in " + description + ".");
                }
                result.put(stringKey, mapValue);
            });
            return result;
        }
        throw new IllegalArgumentException("Expected JSON object for " + description + ".");
    }

    private static Optional<String> optionalStringAllowingBlank(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key)) {
            return Optional.empty();
        }
        Object value = object.get(key);
        if (value instanceof String stringValue) {
            return Optional.of(stringValue);
        }
        throw new IllegalArgumentException("Expected JSON string for " + description + ".");
    }

    private static Optional<Boolean> optionalBoolean(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key)) {
            return Optional.empty();
        }
        Object value = object.get(key);
        if (value instanceof Boolean booleanValue) {
            return Optional.of(booleanValue);
        }
        throw new IllegalArgumentException("Expected JSON boolean for " + description + ".");
    }

    private static Optional<Map<String, Object>> optionalObject(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(requireObject(object.get(key), description));
    }

    private static Map<String, String> toStringMap(Map<String, Object> value) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        value.forEach((key, entryValue) -> {
            if (!(entryValue instanceof String stringValue)) {
                throw new IllegalArgumentException("Expected JSON string for root.resources." + key + ".");
            }
            result.put(key, Validation.requireNonBlank(stringValue, "root.resources." + key + " must not be blank."));
        });
        return result;
    }

    private static Map<ResourceCategory, List<String>> toResourceRootsMap(Map<String, Object> value) {
        EnumMap<ResourceCategory, List<String>> result = new EnumMap<>(ResourceCategory.class);
        value.forEach((key, entryValue) -> {
            ResourceCategory category = ResourceCategory.fromConfigKey(key);
            if (!(entryValue instanceof List<?> list)) {
                throw new IllegalArgumentException(
                        "Expected JSON array for root.resourceRoots." + key + ".");
            }
            List<String> entries = new ArrayList<>();
            int index = 0;
            for (Object element : list) {
                if (!(element instanceof String stringValue) || stringValue.isBlank()) {
                    throw new IllegalArgumentException(
                            "Expected non-blank JSON string for root.resourceRoots." + key + "[" + index + "].");
                }
                entries.add(stringValue);
                index++;
            }
            result.put(category, List.copyOf(entries));
        });
        return result;
    }

    private static Map<ResourceCategory, List<String>> copyResourceRoots(
            Map<ResourceCategory, List<String>> source) {
        EnumMap<ResourceCategory, List<String>> copy = new EnumMap<>(ResourceCategory.class);
        source.forEach((category, paths) -> {
            Validation.requireNonNull(category, "Resource category is required.");
            Validation.requireNonNull(paths, "Resource roots list is required for " + category.configKey() + ".");
            List<String> validated = new ArrayList<>(paths.size());
            int index = 0;
            for (String path : paths) {
                validated.add(Validation.requireNonBlank(path,
                        "Resource root spec must not be blank in " + category.configKey() + "[" + index + "]."));
                index++;
            }
            copy.put(category, List.copyOf(validated));
        });
        return Collections.unmodifiableMap(copy);
    }
}
