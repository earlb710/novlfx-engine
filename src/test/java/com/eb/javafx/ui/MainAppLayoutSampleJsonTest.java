package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips the packaged {@code main-app-layout-test-screen.json} through
 * {@link ScreenDesignJson#load(Path)} and {@link MainAppLayoutPlan#from(ScreenDesignModel)} to
 * catch drift between the sample file shape and the engine parser/loader.
 */
final class MainAppLayoutSampleJsonTest {
    private static final Path SAMPLE_PATH = Paths.get(
            "examples", "resources", "json", "screens", "main-app-layout-test-screen.json");

    @Test
    void sampleLoadsAsMainAppLayoutDesign() {
        ScreenDesignModel design = ScreenDesignJson.load(SAMPLE_PATH);

        assertEquals("test.main-app-layout", design.id());
        assertEquals("Main App Layout Test", design.title());
        assertEquals(ScreenLayoutType.MAIN_APP_LAYOUT, design.layoutType());
        assertEquals(4, design.blocks().size());
    }

    @Test
    void samplePlanReflectsScreenMetadataAndOverlays() {
        MainAppLayoutPlan plan = MainAppLayoutPlan.from(ScreenDesignJson.load(SAMPLE_PATH));

        assertEquals("story", plan.storyScreenId());
        assertEquals("dialog", plan.dialogScreenId());
        assertEquals(MainAppLayoutPlan.DEFAULT_STORY_DIALOG_RATIO, plan.storyDialogRatio(), 1e-9);
        assertEquals(MainAppLayoutOrientation.VERTICAL, plan.orientation());
        assertTrue(plan.showFooter());
        assertEquals("#0d1b2d", plan.backgroundColor());
        assertNotNull(plan.backgroundImage());
        assertEquals(ScreenBackgroundFit.STRETCH, plan.backgroundFit());
        assertEquals(0.8, plan.backgroundOpacity(), 1e-9);

        assertEquals(4, plan.overlays().size());

        MainAppLayoutOverlay status = plan.overlays().get(0);
        assertEquals("hud.status", status.id());
        assertEquals("hud-status", status.screenId());
        assertEquals(MainAppLayoutPlacementMode.ALIGNMENT, status.mode());
        assertEquals(MainAppLayoutAnchor.TOP_LEFT, status.anchor());

        MainAppLayoutOverlay log = plan.overlays().get(1);
        assertEquals(MainAppLayoutAnchor.TOP_RIGHT, log.anchor());
        assertEquals(320.0, log.preferredWidth());
        assertEquals(200.0, log.preferredHeight());

        MainAppLayoutOverlay minimap = plan.overlays().get(2);
        assertEquals(MainAppLayoutPlacementMode.PERCENT, minimap.mode());
        assertEquals(0.82, minimap.offsetX(), 1e-9);
        assertEquals(0.06, minimap.offsetY(), 1e-9);

        MainAppLayoutOverlay toast = plan.overlays().get(3);
        assertEquals(MainAppLayoutPlacementMode.PIXELS, toast.mode());
        assertEquals(320.0, toast.offsetX());
        assertEquals(24.0, toast.offsetY());
    }
}
