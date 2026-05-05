package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void rendererConvertsSafeLineMetadataToInlineFontAndColorStyle() {
        String style = ScreenLayoutRenderer.lineStyle(Map.of(
                "fontFamily", "Serif",
                "fontSize", "22",
                "fontStyle", "bold italic",
                "color", "#66c1e0",
                "backgroundColor", "transparent",
                "transparency", "0.25"));

        assertEquals("-fx-font-family: \"Serif\"; -fx-font-size: 22px; -fx-font-weight: bold; -fx-font-style: italic; -fx-text-fill: #66c1e0; -fx-background-color: transparent; -fx-opacity: 0.75; ", style);
        assertEquals("", ScreenLayoutRenderer.lineStyle(Map.of("color", "red; -fx-padding: 99")));
        assertEquals("", ScreenLayoutRenderer.lineStyle(Map.of("fontFamily", "Serif\"; -fx-padding: 99")));
    }

    @Test
    void rendererConvertsSafeContainerMetadataToBackgroundStyle() {
        assertEquals("-fx-background-color: #143869; -fx-opacity: 0.6; -fx-border-style: dashed; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-border-width: 2px; -fx-border-color: #0099cc; ",
                ScreenLayoutRenderer.containerStyle(Map.of(
                        "backgroundColor", "#143869",
                        "transparency", "0.4",
                        "borderStyle", "dashed",
                        "borderCorner", "rounded",
                        "borderThickness", "2",
                        "borderColor", "#0099cc")));
        assertEquals("",
                ScreenLayoutRenderer.containerStyle(Map.of("backgroundColor", "red; -fx-padding: 99")));
    }

    @Test
    void rendererLeavesBlankMetadataForCssInheritance() {
        assertEquals("", ScreenLayoutRenderer.lineStyle(Map.of(
                "fontFamily", "",
                "fontSize", "",
                "fontStyle", "",
                "color", "")));
        assertEquals("", ScreenLayoutRenderer.containerStyle(Map.of(
                "backgroundColor", "",
                "borderStyle", "",
                "borderCorner", "",
                "borderThickness", "",
                "borderColor", "")));
    }
}
