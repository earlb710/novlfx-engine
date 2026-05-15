package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MainAppLayoutPlanTest {

    @Test
    void parsesScreenMetadataIntoPlanWithDefaults() {
        ScreenDesignModel design = new ScreenDesignModel(
                "main",
                "Main App",
                ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(
                        MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "scene-flow",
                        MainAppLayoutPlan.DIALOG_SCREEN_ID_KEY, "dialog"),
                List.of(),
                List.of(),
                List.of());

        MainAppLayoutPlan plan = MainAppLayoutPlan.from(design);

        assertEquals("Main App", plan.title());
        assertEquals("scene-flow", plan.storyScreenId());
        assertEquals("dialog", plan.dialogScreenId());
        assertEquals(MainAppLayoutPlan.DEFAULT_STORY_DIALOG_RATIO, plan.storyDialogRatio());
        assertEquals(MainAppLayoutOrientation.VERTICAL, plan.orientation());
        assertTrue(plan.showFooter());
        assertNull(plan.backgroundImage());
        assertNull(plan.backgroundColor());
        assertEquals(1.0, plan.backgroundOpacity());
        assertTrue(plan.overlays().isEmpty());
    }

    @Test
    void parsesAllScreenMetadataOverrides() {
        ScreenDesignModel design = new ScreenDesignModel(
                "main",
                "Main App",
                ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(
                        MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story",
                        MainAppLayoutPlan.DIALOG_SCREEN_ID_KEY, "dialog",
                        MainAppLayoutPlan.STORY_DIALOG_RATIO_KEY, "0.6",
                        MainAppLayoutPlan.ORIENTATION_KEY, "horizontal",
                        MainAppLayoutPlan.SHOW_FOOTER_KEY, "false",
                        MainAppLayoutPlan.BACKGROUND_IMAGE_KEY, "bg.png",
                        MainAppLayoutPlan.BACKGROUND_FIT_KEY, "CROP_CENTER",
                        MainAppLayoutPlan.BACKGROUND_TRANSPARENCY_KEY, "0.25",
                        MainAppLayoutPlan.BACKGROUND_COLOR_KEY, "#102030"),
                List.of(),
                List.of(),
                List.of());

        MainAppLayoutPlan plan = MainAppLayoutPlan.from(design);

        assertEquals("story", plan.storyScreenId());
        assertEquals("dialog", plan.dialogScreenId());
        assertEquals(0.6, plan.storyDialogRatio());
        assertEquals(MainAppLayoutOrientation.HORIZONTAL, plan.orientation());
        assertFalse(plan.showFooter());
        assertEquals("bg.png", plan.backgroundImage());
        assertEquals(ScreenBackgroundFit.CROP_CENTER, plan.backgroundFit());
        assertEquals(0.75, plan.backgroundOpacity(), 1e-9);
        assertEquals("#102030", plan.backgroundColor());
    }

    @Test
    void allowsOmittedDialogScreen() {
        ScreenDesignModel design = new ScreenDesignModel(
                "main",
                "Main App",
                ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story"),
                List.of(),
                List.of(),
                List.of());

        MainAppLayoutPlan plan = MainAppLayoutPlan.from(design);

        assertNull(plan.dialogScreenId());
    }

    @Test
    void rejectsScreenDesignsWithWrongLayoutType() {
        ScreenDesignModel design = new ScreenDesignModel(
                "main",
                "Main App",
                ScreenLayoutType.TITLED_PANEL,
                Map.of(MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story"),
                List.of(),
                List.of(),
                List.of());

        assertThrows(IllegalArgumentException.class, () -> MainAppLayoutPlan.from(design));
    }

    @Test
    void requiresStoryScreenIdMetadata() {
        ScreenDesignModel design = new ScreenDesignModel(
                "main",
                "Main App",
                ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(),
                List.of(),
                List.of(),
                List.of());

        assertThrows(IllegalArgumentException.class, () -> MainAppLayoutPlan.from(design));
    }

    @Test
    void rejectsRatioOutOfRange() {
        ScreenDesignModel design = new ScreenDesignModel(
                "main",
                "Main App",
                ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(
                        MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story",
                        MainAppLayoutPlan.STORY_DIALOG_RATIO_KEY, "1.5"),
                List.of(),
                List.of(),
                List.of());

        assertThrows(IllegalArgumentException.class, () -> MainAppLayoutPlan.from(design));
    }

    @Test
    void parsesOverlayBlockWithAlignmentDefaults() {
        ScreenDesignBlock block = new ScreenDesignBlock(
                "status",
                null,
                ScreenLayoutType.HUD_STATUS_OVERLAY,
                null,
                null,
                Map.of(MainAppLayoutPlan.OVERLAY_SCREEN_ID_KEY, "status-screen"));
        ScreenDesignModel design = new ScreenDesignModel(
                "main",
                "Main App",
                ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story"),
                List.of(block),
                List.of(),
                List.of());

        MainAppLayoutPlan plan = MainAppLayoutPlan.from(design);

        assertEquals(1, plan.overlays().size());
        MainAppLayoutOverlay overlay = plan.overlays().get(0);
        assertEquals("status", overlay.id());
        assertEquals("status-screen", overlay.screenId());
        assertEquals(MainAppLayoutPlacementMode.ALIGNMENT, overlay.mode());
        assertEquals(MainAppLayoutAnchor.TOP_LEFT, overlay.anchor());
        assertEquals(0.0, overlay.offsetX());
        assertEquals(0.0, overlay.offsetY());
        assertNull(overlay.preferredWidth());
        assertNull(overlay.preferredHeight());
        assertEquals(1.0, overlay.opacity());
        assertTrue(overlay.visible());
    }

    @Test
    void parsesOverlayPlacementModesAndAnchors() {
        ScreenDesignBlock alignment = new ScreenDesignBlock(
                "status",
                null,
                ScreenLayoutType.HUD_STATUS_OVERLAY,
                null,
                null,
                Map.of(
                        MainAppLayoutPlan.OVERLAY_SCREEN_ID_KEY, "status",
                        MainAppLayoutPlan.OVERLAY_ANCHOR_KEY, "TOP_RIGHT",
                        MainAppLayoutPlan.OVERLAY_OFFSET_X_KEY, "12",
                        MainAppLayoutPlan.OVERLAY_OFFSET_Y_KEY, "8",
                        MainAppLayoutPlan.OVERLAY_WIDTH_KEY, "320",
                        MainAppLayoutPlan.OVERLAY_HEIGHT_KEY, "180",
                        MainAppLayoutPlan.OVERLAY_TRANSPARENCY_KEY, "0.1",
                        MainAppLayoutPlan.OVERLAY_VISIBLE_KEY, "true"));
        ScreenDesignBlock pixels = new ScreenDesignBlock(
                "log",
                null,
                ScreenLayoutType.HUD_STATUS_OVERLAY,
                null,
                null,
                Map.of(
                        MainAppLayoutPlan.OVERLAY_SCREEN_ID_KEY, "log",
                        MainAppLayoutPlan.OVERLAY_PLACEMENT_KEY, "pixels",
                        MainAppLayoutPlan.OVERLAY_OFFSET_X_KEY, "100",
                        MainAppLayoutPlan.OVERLAY_OFFSET_Y_KEY, "50"));
        ScreenDesignBlock percent = new ScreenDesignBlock(
                "minimap",
                null,
                ScreenLayoutType.HUD_STATUS_OVERLAY,
                null,
                null,
                Map.of(
                        MainAppLayoutPlan.OVERLAY_SCREEN_ID_KEY, "minimap",
                        MainAppLayoutPlan.OVERLAY_PLACEMENT_KEY, "percent",
                        MainAppLayoutPlan.OVERLAY_OFFSET_X_KEY, "0.8",
                        MainAppLayoutPlan.OVERLAY_OFFSET_Y_KEY, "0.05"));
        ScreenDesignModel design = new ScreenDesignModel(
                "main",
                "Main App",
                ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story"),
                List.of(alignment, pixels, percent),
                List.of(),
                List.of());

        MainAppLayoutPlan plan = MainAppLayoutPlan.from(design);

        assertEquals(3, plan.overlays().size());
        MainAppLayoutOverlay alignmentOverlay = plan.overlays().get(0);
        assertEquals(MainAppLayoutPlacementMode.ALIGNMENT, alignmentOverlay.mode());
        assertEquals(MainAppLayoutAnchor.TOP_RIGHT, alignmentOverlay.anchor());
        assertEquals(12.0, alignmentOverlay.offsetX());
        assertEquals(8.0, alignmentOverlay.offsetY());
        assertNotNull(alignmentOverlay.preferredWidth());
        assertEquals(320.0, alignmentOverlay.preferredWidth());
        assertEquals(180.0, alignmentOverlay.preferredHeight());
        assertEquals(0.9, alignmentOverlay.opacity(), 1e-9);

        MainAppLayoutOverlay pixelsOverlay = plan.overlays().get(1);
        assertEquals(MainAppLayoutPlacementMode.PIXELS, pixelsOverlay.mode());
        assertNull(pixelsOverlay.anchor());
        assertEquals(100.0, pixelsOverlay.offsetX());

        MainAppLayoutOverlay percentOverlay = plan.overlays().get(2);
        assertEquals(MainAppLayoutPlacementMode.PERCENT, percentOverlay.mode());
        assertEquals(0.8, percentOverlay.offsetX(), 1e-9);
        assertEquals(0.05, percentOverlay.offsetY(), 1e-9);
    }

    @Test
    void rejectsOverlayMissingScreenId() {
        ScreenDesignBlock block = new ScreenDesignBlock(
                "status",
                null,
                ScreenLayoutType.HUD_STATUS_OVERLAY,
                null,
                null,
                Map.of());
        ScreenDesignModel design = new ScreenDesignModel(
                "main",
                "Main App",
                ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story"),
                List.of(block),
                List.of(),
                List.of());

        assertThrows(IllegalArgumentException.class, () -> MainAppLayoutPlan.from(design));
    }

    @Test
    void rejectsPercentOverlayOutOfRange() {
        ScreenDesignBlock block = new ScreenDesignBlock(
                "status",
                null,
                ScreenLayoutType.HUD_STATUS_OVERLAY,
                null,
                null,
                Map.of(
                        MainAppLayoutPlan.OVERLAY_SCREEN_ID_KEY, "status",
                        MainAppLayoutPlan.OVERLAY_PLACEMENT_KEY, "percent",
                        MainAppLayoutPlan.OVERLAY_OFFSET_X_KEY, "1.5",
                        MainAppLayoutPlan.OVERLAY_OFFSET_Y_KEY, "0.5"));
        ScreenDesignModel design = new ScreenDesignModel(
                "main",
                "Main App",
                ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story"),
                List.of(block),
                List.of(),
                List.of());

        assertThrows(IllegalArgumentException.class, () -> MainAppLayoutPlan.from(design));
    }

    @Test
    void anchorParseAcceptsShortAndJavaFxNames() {
        assertEquals(MainAppLayoutAnchor.TOP_CENTER, MainAppLayoutAnchor.parse("TOP"));
        assertEquals(MainAppLayoutAnchor.TOP_CENTER, MainAppLayoutAnchor.parse("top_center"));
        assertEquals(MainAppLayoutAnchor.BOTTOM_RIGHT, MainAppLayoutAnchor.parse("bottom-right"));
        assertEquals(MainAppLayoutAnchor.CENTER, MainAppLayoutAnchor.parse("center"));
        assertThrows(IllegalArgumentException.class, () -> MainAppLayoutAnchor.parse("unknown"));
    }

    @Test
    void parsesRelativeOverlayWithAnchorField() {
        ScreenDesignBlock statusBlock = new ScreenDesignBlock(
                "status", null, ScreenLayoutType.HUD_STATUS_OVERLAY, null, null,
                Map.of(MainAppLayoutPlan.OVERLAY_SCREEN_ID_KEY, "status-screen"));
        ScreenDesignBlock relativeBlock = new ScreenDesignBlock(
                "tooltip", null, ScreenLayoutType.HUD_STATUS_OVERLAY, null, null,
                Map.of(
                        MainAppLayoutPlan.OVERLAY_SCREEN_ID_KEY, "tooltip-screen",
                        MainAppLayoutPlan.OVERLAY_PLACEMENT_KEY, "relative",
                        MainAppLayoutPlan.OVERLAY_ANCHOR_KEY, "LEFT",
                        MainAppLayoutPlan.OVERLAY_ANCHOR_FIELD_KEY, "status",
                        MainAppLayoutPlan.OVERLAY_OFFSET_X_KEY, "-8"));
        ScreenDesignModel design = new ScreenDesignModel(
                "main", "Main App", ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story"),
                List.of(statusBlock, relativeBlock), List.of(), List.of());

        MainAppLayoutPlan plan = MainAppLayoutPlan.from(design);

        MainAppLayoutOverlay relative = plan.overlays().get(1);
        assertEquals(MainAppLayoutPlacementMode.RELATIVE, relative.mode());
        assertEquals(MainAppLayoutAnchor.LEFT, relative.anchor());
        assertEquals("status", relative.anchorBlockId());
        assertEquals(-8.0, relative.offsetX());
    }

    @Test
    void rejectsRelativeOverlayMissingAnchorField() {
        ScreenDesignBlock block = new ScreenDesignBlock(
                "tooltip", null, ScreenLayoutType.HUD_STATUS_OVERLAY, null, null,
                Map.of(
                        MainAppLayoutPlan.OVERLAY_SCREEN_ID_KEY, "tooltip-screen",
                        MainAppLayoutPlan.OVERLAY_PLACEMENT_KEY, "relative",
                        MainAppLayoutPlan.OVERLAY_ANCHOR_KEY, "RIGHT"));
        ScreenDesignModel design = new ScreenDesignModel(
                "main", "Main App", ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story"),
                List.of(block), List.of(), List.of());

        assertThrows(IllegalArgumentException.class, () -> MainAppLayoutPlan.from(design));
    }

    @Test
    void rejectsRelativeOverlayWithNineGridAnchor() {
        ScreenDesignBlock block = new ScreenDesignBlock(
                "tooltip", null, ScreenLayoutType.HUD_STATUS_OVERLAY, null, null,
                Map.of(
                        MainAppLayoutPlan.OVERLAY_SCREEN_ID_KEY, "tooltip-screen",
                        MainAppLayoutPlan.OVERLAY_PLACEMENT_KEY, "relative",
                        MainAppLayoutPlan.OVERLAY_ANCHOR_KEY, "TOP_LEFT",
                        MainAppLayoutPlan.OVERLAY_ANCHOR_FIELD_KEY, "status"));
        ScreenDesignModel design = new ScreenDesignModel(
                "main", "Main App", ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story"),
                List.of(block), List.of(), List.of());

        assertThrows(IllegalArgumentException.class, () -> MainAppLayoutPlan.from(design));
    }

    @Test
    void parsesStoryAndDialogInsetsFromShorthandMetadata() {
        ScreenDesignModel design = new ScreenDesignModel(
                "main", "Main App", ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(
                        MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story",
                        MainAppLayoutPlan.STORY_INSETS_KEY, "8",
                        MainAppLayoutPlan.DIALOG_INSETS_KEY, "4, 12, 4, 12"),
                List.of(), List.of(), List.of());

        MainAppLayoutPlan plan = MainAppLayoutPlan.from(design);

        assertEquals(new MainAppLayoutInsets(8, 8, 8, 8), plan.storyInsets());
        assertEquals(new MainAppLayoutInsets(4, 12, 4, 12), plan.dialogInsets());
    }

    @Test
    void defaultsStoryAndDialogInsetsToEmpty() {
        ScreenDesignModel design = new ScreenDesignModel(
                "main", "Main App", ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story"),
                List.of(), List.of(), List.of());

        MainAppLayoutPlan plan = MainAppLayoutPlan.from(design);

        assertEquals(MainAppLayoutInsets.EMPTY, plan.storyInsets());
        assertEquals(MainAppLayoutInsets.EMPTY, plan.dialogInsets());
    }

    @Test
    void insetsParserAcceptsOneTwoOrFourValuesAndRejectsOthers() {
        assertEquals(new MainAppLayoutInsets(10, 10, 10, 10),
                MainAppLayoutInsets.parse("10", "x"));
        assertEquals(new MainAppLayoutInsets(10, 5, 10, 5),
                MainAppLayoutInsets.parse("10,5", "x"));
        assertEquals(new MainAppLayoutInsets(1, 2, 3, 4),
                MainAppLayoutInsets.parse("1, 2, 3, 4", "x"));
        assertEquals(MainAppLayoutInsets.EMPTY, MainAppLayoutInsets.parse(null, "x"));
        assertEquals(MainAppLayoutInsets.EMPTY, MainAppLayoutInsets.parse("   ", "x"));
        assertThrows(IllegalArgumentException.class, () -> MainAppLayoutInsets.parse("1,2,3", "x"));
        assertThrows(IllegalArgumentException.class, () -> MainAppLayoutInsets.parse("1,2,3,4,5", "x"));
        assertThrows(IllegalArgumentException.class, () -> MainAppLayoutInsets.parse("abc", "x"));
        assertThrows(IllegalArgumentException.class, () -> new MainAppLayoutInsets(-1, 0, 0, 0));
    }

    @Test
    void rejectsAlignmentOverlayWithRelativeAnchor() {
        ScreenDesignBlock block = new ScreenDesignBlock(
                "tooltip", null, ScreenLayoutType.HUD_STATUS_OVERLAY, null, null,
                Map.of(
                        MainAppLayoutPlan.OVERLAY_SCREEN_ID_KEY, "tooltip-screen",
                        MainAppLayoutPlan.OVERLAY_PLACEMENT_KEY, "alignment",
                        MainAppLayoutPlan.OVERLAY_ANCHOR_KEY, "ABOVE"));
        ScreenDesignModel design = new ScreenDesignModel(
                "main", "Main App", ScreenLayoutType.MAIN_APP_LAYOUT,
                Map.of(MainAppLayoutPlan.STORY_SCREEN_ID_KEY, "story"),
                List.of(block), List.of(), List.of());

        assertThrows(IllegalArgumentException.class, () -> MainAppLayoutPlan.from(design));
    }
}
