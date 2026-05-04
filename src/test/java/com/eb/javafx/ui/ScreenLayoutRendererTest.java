package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenLayoutRendererTest {
    @Test
    void rendererExposesStableSemanticStyleHooksForReusableLayouts() {
        assertTrue(ScreenShell.LAYOUT_CONTENT_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_SIDEBAR_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_MAIN_CONTENT_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_ACTION_ROW_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_PRIMARY_ACTION_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_SECONDARY_ACTION_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_CARD_STYLE_CLASS.startsWith("layout-"));
        assertTrue(ScreenShell.LAYOUT_FORM_STYLE_CLASS.startsWith("layout-"));
    }

    @Test
    void rendererValidatesRequiredInputsBeforeCreatingJavaFxControls() {
        ScreenLayoutModel model = new ScreenLayoutModel(
                ScreenLayoutType.TITLED_PANEL,
                "Title",
                null,
                List.of(new ScreenLayoutSection("body", "Body", List.of("Ready"))),
                List.of(),
                List.of(),
                List.of(),
                null);

        assertThrows(IllegalArgumentException.class, () -> ScreenLayoutRenderer.createRoot(null));
        assertThrows(IllegalArgumentException.class, () -> ScreenLayoutRenderer.createRoot(null, null));
        assertThrows(IllegalArgumentException.class, () -> ScreenLayoutRenderer.createScene(null, model));
    }
}
