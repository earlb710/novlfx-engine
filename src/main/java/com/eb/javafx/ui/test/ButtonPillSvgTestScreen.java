package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.ui.ButtonVisuals;
import com.eb.javafx.ui.ScreenShell;
import com.eb.javafx.ui.UiTheme;
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
    static final String DESCRIPTION_TEXT =
            "Use this screen to confirm the shared button-pill.svg shape is visibly applied.";
    static final String DETAIL_TEXT =
            "All buttons below should render with the same pill silhouette from the packaged SVG resource.";
    static final String PRIMARY_LABEL = "Primary action";
    static final String SECONDARY_LABEL = "Secondary action";
    static final String BACK_LABEL = "Back to main menu";

    private ButtonPillSvgTestScreen() {
    }

    public static Scene createScene(
            String title,
            PreferencesService preferencesService,
            UiTheme uiTheme,
            Runnable backAction) {
        Button primary = ButtonVisuals.applySvgArtwork(new Button(PRIMARY_LABEL));
        Button secondary = ButtonVisuals.applySvgArtwork(new Button(SECONDARY_LABEL));
        Button back = ButtonVisuals.applySvgArtwork(new Button(BACK_LABEL));
        back.setOnAction(event -> backAction.run());

        HBox actionRow = new HBox(10, primary, secondary, back);
        VBox content = new VBox(10,
                new Label(DESCRIPTION_TEXT),
                new Label(DETAIL_TEXT),
                actionRow);
        content.setPadding(new Insets(4));

        BorderPane root = ScreenShell.titled(title, content);
        Scene scene = new Scene(root, preferencesService.windowWidth(), preferencesService.windowHeight());
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }
}
