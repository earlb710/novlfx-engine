package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Small manual test route that showcases buttons using the shared button-pill SVG shape.
 */
public final class ButtonPillSvgTestScreen {
    private ButtonPillSvgTestScreen() {
    }

    public static Scene createScene(
            String title,
            PreferencesService preferencesService,
            UiTheme uiTheme,
            Runnable backAction) {
        Button primary = ButtonVisuals.apply(new Button("Primary action"));
        Button secondary = ButtonVisuals.apply(new Button("Secondary action"));
        Button back = ButtonVisuals.apply(new Button("Back to main menu"));
        back.setOnAction(event -> backAction.run());

        HBox actionRow = new HBox(10, primary, secondary, back);
        VBox content = new VBox(10,
                new Label("Use this screen to confirm the shared button-pill.svg shape is visibly applied."),
                new Label("All buttons below should render with the same pill silhouette from the packaged SVG resource."),
                actionRow);
        content.setPadding(new Insets(4));

        BorderPane root = ScreenShell.titled(title, content);
        Scene scene = new Scene(root, preferencesService.windowWidth(), preferencesService.windowHeight());
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }
}
