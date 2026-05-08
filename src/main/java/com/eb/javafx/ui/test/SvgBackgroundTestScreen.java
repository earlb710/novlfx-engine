package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.ui.ButtonVisuals;
import com.eb.javafx.ui.ScreenShell;
import com.eb.javafx.ui.UiTheme;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Manual route that demonstrates the SVG background helper with simple gradient artwork.
 */
public final class SvgBackgroundTestScreen {
    public static final String GRADIENT_BACKGROUND_RESOURCE =
            "/com/eb/javafx/images/svg/background-gradient-rectangle.svg";
    public static final String CIRCLE_BACKGROUND_RESOURCE =
            "/com/eb/javafx/images/svg/circle-background.svg";
    static final String DESCRIPTION_TEXT =
            "Use this screen to confirm the SVG background helper fills the whole scene and preserves gradients.";
    static final String DETAIL_TEXT =
            "Choose a packaged SVG background to compare how simple and complex artwork render behind transparent screen content.";
    static final String BACK_LABEL = "Back to main menu";
    static final String BACKGROUND_LABEL = "Background";
    static final List<BackgroundOption> BACKGROUND_OPTIONS = List.of(
            new BackgroundOption("Gradient rectangle", GRADIENT_BACKGROUND_RESOURCE,
                    "Linear gradient rectangle test background."),
            new BackgroundOption("Circle background", CIRCLE_BACKGROUND_RESOURCE,
                    "Layered radial gradients and circles test background."));

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
        Label backgroundLabel = new Label(BACKGROUND_LABEL);
        ComboBox<BackgroundOption> backgroundChoices = new ComboBox<>(
                FXCollections.observableArrayList(BACKGROUND_OPTIONS));
        backgroundChoices.setMaxWidth(Double.MAX_VALUE);
        backgroundChoices.getSelectionModel().selectFirst();
        details.setText(backgroundChoices.getValue().detailText());
        Button back = ButtonVisuals.applySvgArtwork(new Button(BACK_LABEL));
        back.setOnAction(event -> backAction.run());

        VBox panel = ScreenShell.styledPanel(
                ScreenShell.SCENE_DIALOGUE_PANEL_STYLE_CLASS,
                description,
                backgroundLabel,
                backgroundChoices,
                details,
                new HBox(8, back));

        BorderPane screen = new BorderPane();
        screen.setTop(header);
        screen.setCenter(panel);
        BorderPane.setMargin(header, new Insets(24, 24, 0, 24));
        BorderPane.setMargin(panel, new Insets(0, 24, 24, 24));
        screen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        StackPane root = new StackPane();
        Region background = createBackgroundLayer(backgroundChoices.getValue().resourcePath(), root);
        root.getChildren().addAll(background, screen);
        backgroundChoices.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            details.setText(newValue.detailText());
            root.getChildren().set(0, createBackgroundLayer(newValue.resourcePath(), root));
        });
        return root;
    }

    private static Region createBackgroundLayer(String resourcePath, StackPane root) {
        Region background = ScreenShell.backgroundSvg(resourcePath);
        background.prefWidthProperty().bind(root.widthProperty());
        background.prefHeightProperty().bind(root.heightProperty());
        background.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return background;
    }

    record BackgroundOption(String label, String resourcePath, String detailText) {
        @Override
        public String toString() {
            return label;
        }
    }
}
