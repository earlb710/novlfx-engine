package com.eb.javafx.ui;

import com.eb.javafx.display.DisplayAnimationPlayer;
import com.eb.javafx.display.ImageAssetDefinition;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.display.LayeredCharacterDefinition;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Reusable diagnostic route for display registry bindings and previews.
 *
 * <p>The screen summarizes parsed image aliases, transforms, layered characters, and animation profiles,
 * then renders a limited gallery of display nodes so route authors can verify asset wiring without custom UI.</p>
 */
public final class DisplayBindingsScreen {
    private DisplayBindingsScreen() {
    }

    public static Scene createScene(RouteContext context) {
        ImageDisplayRegistry imageDisplayRegistry = context.imageDisplayRegistry();
        VBox content = new VBox(12);
        content.getChildren().addAll(
                new Label("Parsed image aliases: " + imageDisplayRegistry.images().size()),
                new Label("Parsed transforms: " + imageDisplayRegistry.transforms().size()),
                new Label("Layered character models: " + imageDisplayRegistry.layeredCharacters().size()),
                new Label("Animation profiles: " + imageDisplayRegistry.animations().size()));

        FlowPane previews = new FlowPane(12, 12);
        previews.setPrefWrapLength(680);
        previews.setAlignment(Pos.TOP_LEFT);
        int previewCount = 0;
        for (ImageAssetDefinition definition : imageDisplayRegistry.images().values()) {
            if (previewCount++ >= 12) {
                break;
            }
            VBox tile = new VBox(6);
            tile.setPadding(new Insets(8));
            tile.getStyleClass().add("screen-panel");
            tile.getChildren().addAll(
                    new Label(definition.id() + " [" + definition.layer() + "]"),
                    imageDisplayRegistry.createDisplayNode(definition.id()),
                    new Label(definition.sourcePath()));

            if ("empty_token_small".equals(definition.id())) {
                DisplayAnimationPlayer animationPlayer = new DisplayAnimationPlayer();
                animationPlayer.buildTimeline(tile, imageDisplayRegistry.animation(ImageDisplayRegistry.DISPLAY_PREVIEW_PULSE_ANIMATION),
                        context.preferencesService().reducedMotion()).play();
            }
            previews.getChildren().add(tile);
        }

        VBox layeredCharacterSummary = new VBox(8);
        layeredCharacterSummary.getChildren().add(new Label("Layered display composition"));
        for (LayeredCharacterDefinition definition : imageDisplayRegistry.layeredCharacters().values()) {
            layeredCharacterSummary.getChildren().add(new Label(
                    definition.id() + " -> " + definition.drawOrder()
                            + ", metadata=" + definition.metadata()));
        }

        VBox animationSummary = new VBox(8);
        animationSummary.getChildren().add(new Label("Animation profiles"));
        imageDisplayRegistry.animations().values().forEach(animation ->
                animationSummary.getChildren().add(new Label(
                        animation.id() + " -> steps=" + animation.steps().size()
                                + ", repeat=" + animation.repeatCount()
                                + ", autoReverse=" + animation.autoReverse())));

        HBox sections = new HBox(16, layeredCharacterSummary, animationSummary);
        sections.setAlignment(Pos.TOP_LEFT);

        content.getChildren().addAll(
                previews,
                sections,
                ScreenNavigation.button(context, "Back to main menu", SceneRouter.MAIN_MENU_ROUTE));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return context.themedScene(ScreenShell.titled(
                context.contentRegistry().definition("ui.displayBindings.title"), scrollPane));
    }
}
