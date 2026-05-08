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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Manual route that demonstrates the SVG background helper with simple gradient artwork.
 */
public final class SvgBackgroundTestScreen {
    public static final String BACKGROUND_RESOURCE =
            "/com/eb/javafx/images/svg/background-gradient-rectangle.svg";
    static final String DESCRIPTION_TEXT =
            "Use this screen to confirm the SVG background helper fills the whole scene.";
    static final String DETAIL_TEXT =
            "The background is a packaged SVG gradient rectangle layered behind transparent screen content.";
    static final String BACK_LABEL = "Back to main menu";

    private SvgBackgroundTestScreen() {
    }

    public static Scene createScene(
            String title,
            PreferencesService preferencesService,
            UiTheme uiTheme,
            Runnable backAction) {
        StackPane root = createRoot(title, backAction);
        Scene scene = new Scene(root, preferencesService.windowWidth(), preferencesService.windowHeight());
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }

    static StackPane createRoot(String title, Runnable backAction) {
        Label header = new Label(title);
        header.getStyleClass().add(ScreenShell.SCREEN_TITLE_STYLE_CLASS);

        Label description = new Label(DESCRIPTION_TEXT);
        Label details = new Label(DETAIL_TEXT);
        Button back = ButtonVisuals.applySvgArtwork(new Button(BACK_LABEL));
        back.setOnAction(event -> backAction.run());

        VBox panel = ScreenShell.styledPanel(
                ScreenShell.SCENE_DIALOGUE_PANEL_STYLE_CLASS,
                description,
                details,
                new HBox(8, back));

        BorderPane screen = new BorderPane();
        screen.setTop(header);
        screen.setCenter(panel);
        BorderPane.setMargin(header, ScreenShell.OUTER_INSETS_WITHOUT_BOTTOM);
        BorderPane.setMargin(panel, new Insets(0, 24, 24, 24));

        return ScreenShell.setBackgroundSvg(screen, BACKGROUND_RESOURCE);
    }
}
