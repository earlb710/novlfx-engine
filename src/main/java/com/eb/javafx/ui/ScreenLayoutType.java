package com.eb.javafx.ui;

/**
 * Named reusable layout intents supported by the engine screen layout renderer.
 */
public enum ScreenLayoutType {
    TITLED_PANEL,
    TWO_COLUMN,
    SIDEBAR_CONTENT,
    HUD_STATUS_OVERLAY,
    DIALOGUE,
    MENU_ACTION_LIST,
    FORM,
    PREVIEW_GRID,
    /**
     * App-level scaffolding layout composed of other screens: a background, a story area and dialog
     * area in the central frame (with an optional footer), and any number of HUD overlay screens
     * placed by alignment, pixels, or percent. Rendered by {@code MainAppLayoutRenderer} rather than
     * the standard {@code ScreenLayoutRenderer} because it resolves child screens through a caller-
     * supplied resolver.
     */
    MAIN_APP_LAYOUT
}
