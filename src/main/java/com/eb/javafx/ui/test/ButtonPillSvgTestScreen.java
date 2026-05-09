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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Small manual test route that showcases buttons using shared SVG button shapes.
 */
public final class ButtonPillSvgTestScreen {
    static final String DESCRIPTION_TEXT =
            "Use this screen to confirm the shared SVG button shapes are visibly applied.";
    static final String DETAIL_TEXT =
            "Buttons below cover pill and beveled SVG artwork for dynamic and fixed sizes.";
    static final String PRIMARY_LABEL = "Short";
    static final String SECONDARY_LABEL = "Long dynamic text button";
    static final String FIXED_LABEL = "Fixed width";
    static final String MULTILINE_LABEL = "Multiline\nbutton text";
    static final String BEVEL_LABEL = "Beveled button";
    static final String BACK_LABEL = "Back";

    private ButtonPillSvgTestScreen() {
    }

    public static Scene createScene(
            String title,
            PreferencesService preferencesService,
            UiTheme uiTheme,
            Runnable backAction) {
        Button primary = createSvgButton(PRIMARY_LABEL);
        Button secondary = createSvgButton(SECONDARY_LABEL);
        Button fixed = new Button(FIXED_LABEL);
        fixed.setPrefSize(260, 64);
        ButtonVisuals.applySvgArtwork(fixed);
        Button multiline = createSvgButton(MULTILINE_LABEL);
        Button bevel = ButtonVisuals.applyBevelSvgArtwork(new Button(BEVEL_LABEL));
        Button back = ButtonVisuals.applySvgArtwork(new Button(BACK_LABEL));
        back.setOnAction(event -> backAction.run());

        FlowPane dynamicRow = new FlowPane(10, 10, primary, secondary, multiline);
        HBox fixedRow = new HBox(10, fixed, bevel, back);
        VBox content = new VBox(10,
                new Label(DESCRIPTION_TEXT),
                new Label(DETAIL_TEXT),
                new Label("Dynamic SVG buttons should fit their labels:"),
                dynamicRow,
                new Label("Fixed-size and alternate-shape SVG buttons should rasterize into their requested size:"),
                fixedRow);
        content.setPadding(new Insets(4));

        BorderPane root = ScreenShell.titled(title, content);
        Scene scene = new Scene(root, TestUiScreenSize.sceneWidth(preferencesService), TestUiScreenSize.sceneHeight(preferencesService));
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }

    private static Button createSvgButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        return ButtonVisuals.applySvgArtwork(button);
    }
}
