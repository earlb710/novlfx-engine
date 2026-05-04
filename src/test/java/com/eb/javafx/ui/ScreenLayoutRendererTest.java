package com.eb.javafx.ui;

import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenLayoutRendererTest {
    @Test
    void rendersTwoColumnLayoutWithSemanticStyleClasses() {
        ScreenLayoutModel model = new ScreenLayoutModel(
                ScreenLayoutType.TWO_COLUMN,
                "Equipment",
                "Compare loadouts",
                List.of(
                        new ScreenLayoutSection("left", "Current", List.of("Sword"), "custom-left"),
                        new ScreenLayoutSection("right", "Preview", List.of("Axe"))),
                List.of(new ScreenActionViewModel("Equip", "equip", true)),
                List.of(new ScreenActionViewModel("Cancel", "cancel", false)),
                List.of(),
                "Footer");

        BorderPane root = ScreenLayoutRenderer.createRoot(model);
        VBox panel = (VBox) root.getCenter();
        VBox content = (VBox) panel.getChildren().get(0);
        HBox columns = (HBox) content.getChildren().get(1);
        VBox firstColumn = (VBox) columns.getChildren().get(0);
        VBox secondaryActions = (VBox) content.getChildren().get(3);
        Button disabledSecondary = (Button) secondaryActions.getChildren().get(0);

        assertTrue(root.getStyleClass().contains(ScreenShell.SCREEN_ROOT_STYLE_CLASS));
        assertTrue(panel.getStyleClass().contains(ScreenShell.SCREEN_PANEL_STYLE_CLASS));
        assertTrue(content.getStyleClass().contains(ScreenShell.LAYOUT_CONTENT_STYLE_CLASS));
        assertTrue(columns.getStyleClass().contains(ScreenShell.LAYOUT_TWO_COLUMN_STYLE_CLASS));
        assertTrue(firstColumn.getStyleClass().contains(ScreenShell.LAYOUT_COLUMN_STYLE_CLASS));
        assertTrue(firstColumn.getStyleClass().contains("custom-left"));
        assertTrue(disabledSecondary.isDisabled());
    }

    @Test
    void rendersSidebarEntriesSeparatelyFromMainContent() {
        ScreenLayoutModel model = new ScreenLayoutModel(
                ScreenLayoutType.SIDEBAR_CONTENT,
                "Codex",
                null,
                List.of(new ScreenLayoutSection("entry", "Entry", List.of("Details"))),
                List.of(),
                List.of(),
                List.of(new ScreenActionViewModel("Overview", "overview", true)),
                null);

        BorderPane root = ScreenLayoutRenderer.createRoot(model);
        VBox panel = (VBox) root.getCenter();
        VBox content = (VBox) panel.getChildren().get(0);
        HBox sidebarLayout = (HBox) content.getChildren().get(0);
        VBox sidebar = (VBox) sidebarLayout.getChildren().get(0);
        Button entry = (Button) sidebar.getChildren().get(0);

        assertTrue(sidebarLayout.getStyleClass().contains(ScreenShell.LAYOUT_SIDEBAR_CONTENT_STYLE_CLASS));
        assertTrue(sidebar.getStyleClass().contains(ScreenShell.LAYOUT_SIDEBAR_STYLE_CLASS));
        assertTrue(entry.getStyleClass().contains(ScreenShell.LAYOUT_SIDEBAR_ENTRY_STYLE_CLASS));
        assertEquals("Overview", entry.getText());
    }
}
