package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * UI-neutral parsed plan for a {@link ScreenLayoutType#MAIN_APP_LAYOUT} screen design.
 *
 * <p>The plan extracts the three structural parts of a typical visual-novel app frame from the
 * design's screen metadata and blocks:</p>
 * <ul>
 *   <li><b>Background</b>: image path, fit mode, transparency, and background colour pulled from
 *       screen-level metadata. The renderer paints these behind all other content.</li>
 *   <li><b>Main central frame</b>: a story screen and an optional dialog screen referenced by id,
 *       sharing the central area according to a fixed ratio and orientation. An optional footer
 *       sits below the frame.</li>
 *   <li><b>HUD overlays</b>: zero or more {@link MainAppLayoutOverlay} entries (one per block in
 *       the design), each binding a screen id to a placement on the overlay layer.</li>
 * </ul>
 *
 * <p>The plan does not touch JavaFX, so it is trivially unit-testable. The renderer
 * ({@code MainAppLayoutRenderer}) turns the plan plus a {@link MainAppScreenResolver} into a
 * JavaFX node tree.</p>
 */
public record MainAppLayoutPlan(
        String title,
        String backgroundImage,
        ScreenBackgroundFit backgroundFit,
        double backgroundOpacity,
        String backgroundColor,
        String storyScreenId,
        String dialogScreenId,
        double storyDialogRatio,
        MainAppLayoutOrientation orientation,
        boolean showFooter,
        MainAppLayoutInsets storyInsets,
        MainAppLayoutInsets dialogInsets,
        List<MainAppLayoutOverlay> overlays) {

    public static final String BACKGROUND_IMAGE_KEY = "appLayoutBackgroundImage";
    public static final String BACKGROUND_FIT_KEY = "appLayoutBackgroundFit";
    public static final String BACKGROUND_TRANSPARENCY_KEY = "appLayoutBackgroundTransparency";
    public static final String BACKGROUND_COLOR_KEY = "appLayoutBackgroundColor";
    public static final String STORY_SCREEN_ID_KEY = "storyScreenId";
    public static final String DIALOG_SCREEN_ID_KEY = "dialogScreenId";
    public static final String STORY_DIALOG_RATIO_KEY = "storyDialogRatio";
    public static final String ORIENTATION_KEY = "appLayoutOrientation";
    public static final String SHOW_FOOTER_KEY = "showFooter";
    /** CSS-style shorthand padding applied inside the story slot (e.g. {@code "10"} or {@code "8,12,8,12"}). */
    public static final String STORY_INSETS_KEY = "storyInsets";
    /** CSS-style shorthand padding applied inside the dialog slot. */
    public static final String DIALOG_INSETS_KEY = "dialogInsets";

    public static final String OVERLAY_SCREEN_ID_KEY = "overlayScreenId";
    public static final String OVERLAY_PLACEMENT_KEY = "overlayPlacement";
    public static final String OVERLAY_ANCHOR_KEY = "overlayAnchor";
    /** Block id used as the relative anchor target when {@code overlayPlacement} is {@code relative}. */
    public static final String OVERLAY_ANCHOR_FIELD_KEY = "overlayAnchorField";
    public static final String OVERLAY_OFFSET_X_KEY = "overlayOffsetX";
    public static final String OVERLAY_OFFSET_Y_KEY = "overlayOffsetY";
    public static final String OVERLAY_WIDTH_KEY = "overlayWidth";
    public static final String OVERLAY_HEIGHT_KEY = "overlayHeight";
    public static final String OVERLAY_TRANSPARENCY_KEY = "overlayTransparency";
    public static final String OVERLAY_VISIBLE_KEY = "overlayVisible";

    public static final double DEFAULT_STORY_DIALOG_RATIO = 0.5;

    public MainAppLayoutPlan {
        title = Validation.requireNonBlank(title, "Main app layout title is required.");
        storyScreenId = Validation.requireNonBlank(storyScreenId, "Main app layout storyScreenId is required.");
        if (dialogScreenId != null && dialogScreenId.isBlank()) {
            throw new IllegalArgumentException("Main app layout dialogScreenId cannot be blank.");
        }
        Validation.requireBetween(storyDialogRatio, 0.0, 1.0,
                "Main app layout storyDialogRatio must be between 0.0 and 1.0.");
        Validation.requireUnitInterval(backgroundOpacity,
                "Main app layout background opacity must be between 0.0 and 1.0.");
        orientation = orientation == null ? MainAppLayoutOrientation.VERTICAL : orientation;
        backgroundFit = backgroundFit == null ? ScreenBackgroundFit.STRETCH : backgroundFit;
        storyInsets = storyInsets == null ? MainAppLayoutInsets.EMPTY : storyInsets;
        dialogInsets = dialogInsets == null ? MainAppLayoutInsets.EMPTY : dialogInsets;
        overlays = List.copyOf(Validation.requireNonNull(overlays, "Main app layout overlays are required."));
        Set<String> overlayIds = new LinkedHashSet<>();
        for (MainAppLayoutOverlay overlay : overlays) {
            Validation.requireNonNull(overlay, "Main app layout overlay is required.");
            if (!overlayIds.add(overlay.id())) {
                throw new IllegalArgumentException(
                        "Duplicate main app layout overlay id: " + overlay.id());
            }
        }
    }

    /**
     * Parses a {@link ScreenDesignModel} into a structural plan.
     *
     * @param design design with {@link ScreenLayoutType#MAIN_APP_LAYOUT}
     * @return parsed plan describing background, central frame, and HUD overlays
     * @throws IllegalArgumentException if {@code design} is not a main-app-layout design
     */
    public static MainAppLayoutPlan from(ScreenDesignModel design) {
        Validation.requireNonNull(design, "Screen design is required.");
        if (design.layoutType() != ScreenLayoutType.MAIN_APP_LAYOUT) {
            throw new IllegalArgumentException(
                    "Screen design layoutType must be MAIN_APP_LAYOUT, found: " + design.layoutType());
        }
        List<MainAppLayoutOverlay> overlays = new ArrayList<>();
        for (ScreenDesignBlock block : design.blocks()) {
            overlays.add(parseOverlay(block.id(), block.metadata()));
        }
        return buildPlan(design.title(), design.metadata(), overlays);
    }

    /**
     * Parses a post-adapter {@link ScreenLayoutModel} into a structural plan.
     *
     * <p>This is the entry point for designer-preview rendering, which has already converted the
     * authored {@link ScreenDesignModel} into a layout model via {@code ScreenDesignLayoutAdapter}.
     * Overlay blocks survive the conversion as {@link ScreenLayoutSection}s carrying the same
     * metadata, so the parsing logic is shared with {@link #from(ScreenDesignModel)}.</p>
     */
    public static MainAppLayoutPlan fromLayoutModel(ScreenLayoutModel model) {
        Validation.requireNonNull(model, "Screen layout model is required.");
        if (model.type() != ScreenLayoutType.MAIN_APP_LAYOUT) {
            throw new IllegalArgumentException(
                    "Screen layout type must be MAIN_APP_LAYOUT, found: " + model.type());
        }
        List<MainAppLayoutOverlay> overlays = new ArrayList<>();
        for (ScreenLayoutSection section : model.contentSections()) {
            if (section.metadata().get(OVERLAY_SCREEN_ID_KEY) == null) {
                continue;
            }
            overlays.add(parseOverlay(section.id(), section.metadata()));
        }
        return buildPlan(model.title(), model.metadata(), overlays);
    }

    private static MainAppLayoutPlan buildPlan(
            String title,
            Map<String, String> metadata,
            List<MainAppLayoutOverlay> overlays) {
        return new MainAppLayoutPlan(
                title,
                trimmedOrNull(metadata.get(BACKGROUND_IMAGE_KEY)),
                parseFit(metadata.get(BACKGROUND_FIT_KEY)),
                opacityFromTransparency(metadata.get(BACKGROUND_TRANSPARENCY_KEY), 1.0),
                trimmedOrNull(metadata.get(BACKGROUND_COLOR_KEY)),
                requireMetadata(metadata, STORY_SCREEN_ID_KEY,
                        "Main app layout storyScreenId metadata is required."),
                trimmedOrNull(metadata.get(DIALOG_SCREEN_ID_KEY)),
                parseRatio(metadata.get(STORY_DIALOG_RATIO_KEY), DEFAULT_STORY_DIALOG_RATIO),
                MainAppLayoutOrientation.parse(metadata.get(ORIENTATION_KEY)),
                parseBoolean(metadata.get(SHOW_FOOTER_KEY), true),
                MainAppLayoutInsets.parse(metadata.get(STORY_INSETS_KEY), "Main app layout storyInsets"),
                MainAppLayoutInsets.parse(metadata.get(DIALOG_INSETS_KEY), "Main app layout dialogInsets"),
                List.copyOf(overlays));
    }

    private static MainAppLayoutOverlay parseOverlay(String ownerId, Map<String, String> metadata) {
        String screenId = requireMetadata(metadata, OVERLAY_SCREEN_ID_KEY,
                "Main app layout overlay '" + ownerId + "' missing overlayScreenId metadata.");
        MainAppLayoutPlacementMode mode = MainAppLayoutPlacementMode.parse(metadata.get(OVERLAY_PLACEMENT_KEY));
        MainAppLayoutAnchor anchor = switch (mode) {
            case ALIGNMENT -> MainAppLayoutAnchor.parse(metadata.getOrDefault(OVERLAY_ANCHOR_KEY, "TOP_LEFT"));
            case RELATIVE -> MainAppLayoutAnchor.parse(metadata.getOrDefault(OVERLAY_ANCHOR_KEY, "RIGHT"));
            case PIXELS, PERCENT -> null;
        };
        String anchorBlockId = trimmedOrNull(metadata.get(OVERLAY_ANCHOR_FIELD_KEY));
        double offsetX = parseDouble(metadata.get(OVERLAY_OFFSET_X_KEY), 0.0,
                "Main app layout overlay '" + ownerId + "' offsetX must be a number.");
        double offsetY = parseDouble(metadata.get(OVERLAY_OFFSET_Y_KEY), 0.0,
                "Main app layout overlay '" + ownerId + "' offsetY must be a number.");
        Double preferredWidth = parseOptionalDouble(metadata.get(OVERLAY_WIDTH_KEY),
                "Main app layout overlay '" + ownerId + "' width must be a number.");
        Double preferredHeight = parseOptionalDouble(metadata.get(OVERLAY_HEIGHT_KEY),
                "Main app layout overlay '" + ownerId + "' height must be a number.");
        double opacity = opacityFromTransparency(metadata.get(OVERLAY_TRANSPARENCY_KEY), 1.0);
        boolean visible = parseBoolean(metadata.get(OVERLAY_VISIBLE_KEY), true);
        return new MainAppLayoutOverlay(ownerId, screenId, mode, anchor, anchorBlockId,
                offsetX, offsetY, preferredWidth, preferredHeight, opacity, visible);
    }

    private static String trimmedOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String requireMetadata(Map<String, String> metadata, String key, String message) {
        String value = trimmedOrNull(metadata.get(key));
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static ScreenBackgroundFit parseFit(String value) {
        if (value == null || value.isBlank()) {
            return ScreenBackgroundFit.STRETCH;
        }
        try {
            return ScreenBackgroundFit.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unknown main-app-layout background fit: " + value, exception);
        }
    }

    private static double parseRatio(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            return Validation.requireBetween(parsed, 0.0, 1.0,
                    "Main app layout storyDialogRatio must be between 0.0 and 1.0.");
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Main app layout storyDialogRatio must be a number, found: " + value, exception);
        }
    }

    private static double opacityFromTransparency(String value, double defaultOpacity) {
        if (value == null || value.isBlank()) {
            return defaultOpacity;
        }
        try {
            double transparency = Double.parseDouble(value.trim());
            return Validation.requireUnitInterval(1.0 - transparency,
                    "Main app layout transparency must be between 0.0 and 1.0.");
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Main app layout transparency must be a number, found: " + value, exception);
        }
    }

    private static double parseDouble(String value, double defaultValue, String errorMessage) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(errorMessage, exception);
        }
    }

    private static Double parseOptionalDouble(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(errorMessage, exception);
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "y", "on" -> true;
            case "false", "0", "no", "n", "off" -> false;
            default -> defaultValue;
        };
    }
}
